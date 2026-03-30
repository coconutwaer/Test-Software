#!/usr/bin/env python3
"""
auto_crop.py — Batch background removal for product images.

Pipeline per image:
1.  Load image → RGB
2.  Run dual AI models (birefnet-general + u2net) → merge masks with np.maximum
3.  Check coverage percentage
4a. IF coverage > 85%: trim white borders → resize → save as RGBA
4b. IF coverage <= 85%:
      a. Fragment fill (bounding box between large fragments)
      b. Build smooth alpha (keep AI edge values, inner = 255)
      c. Apply alpha to original image
      d. Remove white halo (pure-white check via HSV saturation)
      e. Crop with padding
      f. Add glow
      g. Resize if needed
      h. Save as PNG
"""

import argparse
import gc
import hashlib
import io
import json
import logging
import sys
import time
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from tqdm import tqdm

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".tiff", ".tif", ".webp"}
TRACKER_FILENAME = "auto_crop_tracker.json"
LOG_FILENAME = "auto_crop.log"
SAVE_TRACKER_EVERY = 20
GC_EVERY = 20
MAX_LONG_SIDE = 1200
COVERAGE_THRESHOLD = 0.85  # 85%
FRAGMENT_MIN_AREA_RATIO = 0.003  # 0.3 % of total image area
WHITE_TRIM_BRIGHTNESS = 240
WHITE_TRIM_PADDING = 0.03  # 3 %
CROP_PADDING_MIN_PX = 20
CROP_PADDING_RATIO = 0.08  # 8 %
INNER_ALPHA_THRESHOLD = 200
HALO_BRIGHTNESS_THRESHOLD = 245
HALO_SATURATION_THRESHOLD = 30  # HSV scale 0-255

# ---------------------------------------------------------------------------
# Logging setup
# ---------------------------------------------------------------------------

logger = logging.getLogger("auto_crop")


def setup_logging(log_path: Path) -> None:
    logger.setLevel(logging.DEBUG)
    fmt = logging.Formatter("%(asctime)s [%(levelname)s] %(message)s", datefmt="%H:%M:%S")

    sh = logging.StreamHandler(sys.stdout)
    sh.setLevel(logging.INFO)
    sh.setFormatter(fmt)

    fh = logging.FileHandler(log_path, encoding="utf-8")
    fh.setLevel(logging.DEBUG)
    fh.setFormatter(fmt)

    logger.addHandler(sh)
    logger.addHandler(fh)


# ---------------------------------------------------------------------------
# MD5 helper
# ---------------------------------------------------------------------------

def file_md5(path: Path) -> str:
    h = hashlib.md5(usedforsecurity=False)
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


# ---------------------------------------------------------------------------
# Tracker
# ---------------------------------------------------------------------------

class Tracker:
    def __init__(self, path: Path) -> None:
        self.path = path
        self.data: dict[str, str] = {}  # filename → md5
        if path.exists():
            try:
                with open(path, "r", encoding="utf-8") as f:
                    self.data = json.load(f)
            except Exception:
                self.data = {}

    def is_done(self, filepath: Path) -> bool:
        key = filepath.name
        if key not in self.data:
            return False
        try:
            return self.data[key] == file_md5(filepath)
        except Exception:
            return False

    def mark_done(self, filepath: Path) -> None:
        self.data[filepath.name] = file_md5(filepath)

    def save(self) -> None:
        with open(self.path, "w", encoding="utf-8") as f:
            json.dump(self.data, f, indent=2)

    def reset(self) -> None:
        self.data = {}
        if self.path.exists():
            self.path.unlink()


# ---------------------------------------------------------------------------
# Model loading
# ---------------------------------------------------------------------------

_session_birefnet = None
_session_u2net = None


def load_models() -> None:
    global _session_birefnet, _session_u2net
    logger.info("Loading AI models…")
    from rembg import new_session

    _session_birefnet = new_session("birefnet-general")
    logger.info("  birefnet-general loaded")
    _session_u2net = new_session("u2net")
    logger.info("  u2net loaded")


# ---------------------------------------------------------------------------
# Core image processing helpers
# ---------------------------------------------------------------------------


def get_alpha_mask(img_rgb: Image.Image, session) -> np.ndarray:
    """Return grayscale alpha mask (H×W uint8) from rembg."""
    from rembg import remove as rembg_remove

    buf = io.BytesIO()
    img_rgb.save(buf, format="PNG")
    buf.seek(0)
    result_bytes = rembg_remove(buf.read(), session=session)
    result = Image.open(io.BytesIO(result_bytes)).convert("RGBA")
    return np.array(result)[:, :, 3]


def dual_model_mask(img_rgb: Image.Image) -> np.ndarray:
    """Run both models, return merged alpha mask (H×W uint8) with noise removed."""
    mask_a = get_alpha_mask(img_rgb, _session_birefnet)
    mask_b = get_alpha_mask(img_rgb, _session_u2net)
    merged = np.maximum(mask_a, mask_b)

    # Remove small noise fragments via morphological open (erode then dilate)
    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
    binary = (merged > 10).astype(np.uint8)
    cleaned = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel, iterations=1)
    # Zero out pixels that were noise (present before, removed after open)
    noise_mask = (binary > 0) & (cleaned == 0)
    merged[noise_mask] = 0

    return merged


# ---------------------------------------------------------------------------
# Step 3 – White border trimming
# ---------------------------------------------------------------------------

def trim_white_borders(img_rgb: Image.Image) -> Image.Image:
    """Trim white borders using line-based row/column brightness scan."""
    arr = np.array(img_rgb.convert("RGB"), dtype=np.float32)
    h, w = arr.shape[:2]

    row_means = arr.mean(axis=(1, 2))   # mean brightness per row
    col_means = arr.mean(axis=(0, 2))   # mean brightness per col

    row_hits = np.where(row_means < WHITE_TRIM_BRIGHTNESS)[0]
    col_hits = np.where(col_means < WHITE_TRIM_BRIGHTNESS)[0]

    if len(row_hits) == 0 or len(col_hits) == 0:
        return img_rgb  # nothing to trim

    top, bottom = int(row_hits[0]), int(row_hits[-1])
    left, right = int(col_hits[0]), int(col_hits[-1])

    pad_y = max(1, int((bottom - top) * WHITE_TRIM_PADDING))
    pad_x = max(1, int((right - left) * WHITE_TRIM_PADDING))

    top = max(0, top - pad_y)
    bottom = min(h - 1, bottom + pad_y)
    left = max(0, left - pad_x)
    right = min(w - 1, right + pad_x)

    return img_rgb.crop((left, top, right + 1, bottom + 1))


# ---------------------------------------------------------------------------
# Step 4 – Fragment filling
# ---------------------------------------------------------------------------

def fill_fragments(mask: np.ndarray) -> np.ndarray:
    """Fill bounding box between large fragments with full opacity."""
    binary = (mask > 1).astype(np.uint8)
    num_labels, labels, stats, _ = cv2.connectedComponentsWithStats(binary, connectivity=8)

    h, w = mask.shape
    total_px = h * w
    min_area = total_px * FRAGMENT_MIN_AREA_RATIO

    large_indices = [
        i for i in range(1, num_labels)
        if stats[i, cv2.CC_STAT_AREA] >= min_area
    ]

    if len(large_indices) < 2:
        return mask

    # Bounding box enclosing all large fragments
    x_min = min(stats[i, cv2.CC_STAT_LEFT] for i in large_indices)
    y_min = min(stats[i, cv2.CC_STAT_TOP] for i in large_indices)
    x_max = max(stats[i, cv2.CC_STAT_LEFT] + stats[i, cv2.CC_STAT_WIDTH] for i in large_indices)
    y_max = max(stats[i, cv2.CC_STAT_TOP] + stats[i, cv2.CC_STAT_HEIGHT] for i in large_indices)

    filled = mask.copy()
    filled[y_min:y_max, x_min:x_max] = np.maximum(
        filled[y_min:y_max, x_min:x_max], 255
    )
    return filled


# ---------------------------------------------------------------------------
# Step 5 – Smooth alpha
# ---------------------------------------------------------------------------

def build_smooth_alpha(mask: np.ndarray) -> np.ndarray:
    """Keep AI edge gradients; set inner high-confidence region to 255; blur edge band."""
    smooth = mask.copy().astype(np.float32)

    inner = mask > INNER_ALPHA_THRESHOLD
    smooth[inner] = 255.0

    # Edge band: region between any detection and the inner area
    edge_band = (mask > 1) & ~inner
    if edge_band.any():
        blurred = cv2.GaussianBlur(smooth, (9, 9), sigmaX=2.0, sigmaY=2.0)
        smooth[edge_band] = blurred[edge_band]

    return np.clip(smooth, 0, 255).astype(np.uint8)


# ---------------------------------------------------------------------------
# Step 6 – White halo removal
# ---------------------------------------------------------------------------

def remove_white_halo(rgba_arr: np.ndarray) -> np.ndarray:
    """
    Check the 1-px border of the current alpha mask.
    Remove pixels that are pure white (brightness > 245 AND saturation < 30 in HSV).
    """
    alpha = rgba_arr[:, :, 3].copy()
    rgb = rgba_arr[:, :, :3]

    # HSV for saturation check
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    value = hsv[:, :, 2].astype(np.float32)
    saturation = hsv[:, :, 1].astype(np.float32)

    # 1-px erode mask to find the border ring
    kernel = np.ones((3, 3), np.uint8)
    eroded = cv2.erode((alpha > 0).astype(np.uint8), kernel, iterations=1)
    border_ring = ((alpha > 0).astype(np.uint8) - eroded).astype(bool)

    pure_white = (value > HALO_BRIGHTNESS_THRESHOLD) & (saturation < HALO_SATURATION_THRESHOLD)
    halo_pixels = border_ring & pure_white

    alpha[halo_pixels] = 0

    # Smooth new edge
    alpha_f = alpha.astype(np.float32)
    alpha_f = cv2.GaussianBlur(alpha_f, (5, 5), sigmaX=1.0, sigmaY=1.0)
    alpha = np.clip(alpha_f, 0, 255).astype(np.uint8)

    result = rgba_arr.copy()
    result[:, :, 3] = alpha
    return result


# ---------------------------------------------------------------------------
# Step 7 – Crop with padding
# ---------------------------------------------------------------------------

def crop_with_padding(img_rgba: Image.Image) -> Image.Image:
    arr = np.array(img_rgba)
    alpha = arr[:, :, 3]
    rows = np.any(alpha > 0, axis=1)
    cols = np.any(alpha > 0, axis=0)

    if not rows.any():
        return img_rgba

    top, bottom = int(np.where(rows)[0][0]), int(np.where(rows)[0][-1])
    left, right = int(np.where(cols)[0][0]), int(np.where(cols)[0][-1])

    prod_h = bottom - top
    prod_w = right - left
    pad = max(CROP_PADDING_MIN_PX, int(max(prod_h, prod_w) * CROP_PADDING_RATIO))

    h, w = alpha.shape
    top = max(0, top - pad)
    bottom = min(h - 1, bottom + pad)
    left = max(0, left - pad)
    right = min(w - 1, right + pad)

    return img_rgba.crop((left, top, right + 1, bottom + 1))


# ---------------------------------------------------------------------------
# Step 8 – Glow effect
# ---------------------------------------------------------------------------

def add_glow(img_rgba: Image.Image) -> Image.Image:
    w, h = img_rgba.size
    longest = max(w, h)

    if longest < 200:
        radius, opacity = 2, 18
    elif longest < 500:
        radius, opacity = 3, 22
    else:
        radius, opacity = 4, 25

    arr = np.array(img_rgba).astype(np.float32)
    alpha = arr[:, :, 3]

    # Build glow: Gaussian blur of the alpha channel, scaled by opacity
    ksize = radius * 2 + 1
    glow_alpha = cv2.GaussianBlur(alpha, (ksize, ksize), sigmaX=radius, sigmaY=radius)
    glow_alpha = glow_alpha * (opacity / 255.0)

    # "Over" compositing: glow (white) underneath, product on top
    # Normalised alpha channels (0..1)
    a_prod = alpha / 255.0
    a_glow = glow_alpha / 255.0

    # Combined alpha: a_out = a_prod + a_glow * (1 - a_prod)
    a_out = a_prod + a_glow * (1.0 - a_prod)

    # Avoid division by zero where both are transparent
    safe = np.where(a_out > 0, a_out, 1.0)

    # Per-channel blend: C_out = (C_prod * a_prod + 255 * a_glow * (1 - a_prod)) / a_out
    out = arr.copy()
    for c in range(3):
        out[:, :, c] = (arr[:, :, c] * a_prod + 255.0 * a_glow * (1.0 - a_prod)) / safe

    out[:, :, 3] = np.clip(a_out * 255.0, 0, 255)
    out = np.clip(out, 0, 255)

    return Image.fromarray(out.astype(np.uint8), "RGBA")


# ---------------------------------------------------------------------------
# Step 9 – Resize
# ---------------------------------------------------------------------------

def resize_if_needed(img: Image.Image) -> Image.Image:
    w, h = img.size
    longest = max(w, h)
    if longest <= MAX_LONG_SIDE:
        return img
    scale = MAX_LONG_SIDE / longest
    new_w = max(1, int(round(w * scale)))
    new_h = max(1, int(round(h * scale)))
    return img.resize((new_w, new_h), Image.LANCZOS)


# ---------------------------------------------------------------------------
# Main processing pipeline
# ---------------------------------------------------------------------------

def process_image(input_path: Path, output_path: Path) -> None:
    # 1. Load
    img_rgb = Image.open(input_path).convert("RGB")

    # 2. Dual AI mask
    mask = dual_model_mask(img_rgb)

    # 3. Coverage check
    h, w = mask.shape
    coverage = float(np.count_nonzero(mask > 1)) / (h * w)
    logger.debug(f"  Coverage: {coverage:.1%}")

    if coverage > COVERAGE_THRESHOLD:
        # 4a. High coverage: trim white borders only
        logger.debug("  High coverage → trim white borders")
        trimmed = trim_white_borders(img_rgb)
        trimmed_rgba = trimmed.convert("RGBA")
        trimmed_rgba = resize_if_needed(trimmed_rgba)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        trimmed_rgba.save(output_path, format="PNG", optimize=True, compress_level=9)
        return

    # 4b. Normal background removal pipeline

    # a. Fragment fill
    mask = fill_fragments(mask)

    # b. Smooth alpha
    smooth_alpha = build_smooth_alpha(mask)

    # c. Apply alpha to original
    arr_rgb = np.array(img_rgb)
    h, w = arr_rgb.shape[:2]
    rgba_arr = np.zeros((h, w, 4), dtype=np.uint8)
    rgba_arr[:, :, :3] = arr_rgb
    rgba_arr[:, :, 3] = smooth_alpha

    # d. White halo removal
    rgba_arr = remove_white_halo(rgba_arr)

    # e. Crop with padding
    img_rgba = Image.fromarray(rgba_arr, "RGBA")
    img_rgba = crop_with_padding(img_rgba)

    # f. Glow
    img_rgba = add_glow(img_rgba)

    # g. Resize
    img_rgba = resize_if_needed(img_rgba)

    # h. Save
    output_path.parent.mkdir(parents=True, exist_ok=True)
    img_rgba.save(output_path, format="PNG", optimize=True, compress_level=9)


# ---------------------------------------------------------------------------
# Batch processing
# ---------------------------------------------------------------------------

def collect_images(input_dir: Path) -> list[Path]:
    images = []
    for p in sorted(input_dir.iterdir()):
        if p.is_file() and p.suffix.lower() in SUPPORTED_EXTENSIONS:
            images.append(p)
    return images


def run_batch(input_dir: Path, output_dir: Path, reset: bool) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    tracker_path = output_dir / TRACKER_FILENAME
    tracker = Tracker(tracker_path)

    if reset:
        logger.info("--reset: clearing tracker")
        tracker.reset()

    images = collect_images(input_dir)
    total = len(images)

    pending = [p for p in images if not tracker.is_done(p)]
    done_count = total - len(pending)

    logger.info(f"Images: {total} total | {done_count} already done | {len(pending)} pending")

    if not pending:
        logger.info("Nothing to process.")
        return

    load_models()

    errors: list[str] = []
    start_time = time.time()
    processed = 0

    with tqdm(total=len(pending), unit="img", desc="Processing") as pbar:
        for idx, src_path in enumerate(pending, 1):
            stem = src_path.stem
            dst_path = output_dir / f"{stem}.png"

            try:
                t0 = time.time()
                process_image(src_path, dst_path)
                elapsed = time.time() - t0
                logger.debug(f"  OK {src_path.name} ({elapsed:.1f}s)")
                tracker.mark_done(src_path)
                processed += 1
            except Exception as exc:
                logger.error(f"  FAIL {src_path.name}: {exc}")
                errors.append(src_path.name)

            pbar.update(1)

            if idx % SAVE_TRACKER_EVERY == 0:
                tracker.save()
                logger.debug("  Tracker saved")

            if idx % GC_EVERY == 0:
                gc.collect()

    tracker.save()

    total_time = time.time() - start_time
    avg = total_time / processed if processed else 0
    logger.info(
        f"Done. Processed {processed}/{len(pending)} images in {total_time:.1f}s "
        f"(avg {avg:.1f}s/img)"
    )
    if errors:
        logger.warning(f"Errors ({len(errors)}): {', '.join(errors)}")


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Batch background removal for product images."
    )
    parser.add_argument(
        "-i", "--input",
        default="./input",
        help="Input directory (default: ./input)",
    )
    parser.add_argument(
        "-o", "--output",
        default="./output",
        help="Output directory (default: ./output)",
    )
    parser.add_argument(
        "--reset",
        action="store_true",
        help="Clear tracker and reprocess all images",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    input_dir = Path(args.input)
    output_dir = Path(args.output)
    log_path = output_dir / LOG_FILENAME
    output_dir.mkdir(parents=True, exist_ok=True)
    setup_logging(log_path)

    if not input_dir.exists():
        logger.error(f"Input directory does not exist: {input_dir}")
        sys.exit(1)

    logger.info(f"Input:  {input_dir.resolve()}")
    logger.info(f"Output: {output_dir.resolve()}")

    run_batch(input_dir, output_dir, reset=args.reset)


if __name__ == "__main__":
    main()
