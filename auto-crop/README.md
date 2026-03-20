# Auto-Crop — Batch Background Removal for Product Images

A Python tool that removes backgrounds from product images (food, packaging, candy, etc.) and outputs clean PNGs with transparent backgrounds.

---

## Features

- **Dual AI model strategy** (`birefnet-general` + `u2net`) for robust mask generation
- **Smart coverage detection** — transparent/glass packaging is handled gracefully
- **Fragment filling** — white packaging pieces are kept together
- **Smooth edges** — no jagged or pixelated transitions
- **White halo removal** — removes white fringe while preserving metallic surfaces
- **Glow effect** — subtle white glow for clean product presentation
- **Batch processing** with JSON-based resume tracking

---

## Installation

```bash
cd auto-crop
pip install -r requirements.txt
```

> **Note:** `rembg[gpu]` requires CUDA-enabled hardware to use the GPU. On CPU-only systems, rembg falls back to CPU automatically.

---

## Usage

```bash
# Basic usage (reads from ./input, writes to ./output)
python auto_crop.py

# Custom input/output directories
python auto_crop.py --input /path/to/images --output /path/to/results

# Short flags
python auto_crop.py -i ./raw -o ./clean

# Reset tracker and reprocess all images
python auto_crop.py --reset
```

### CLI Arguments

| Argument | Short | Default | Description |
|---|---|---|---|
| `--input` | `-i` | `./input` | Directory containing source images |
| `--output` | `-o` | `./output` | Directory for processed output images |
| `--reset` | — | `false` | Clear tracker and reprocess everything |

---

## Supported Input Formats

`.jpg` · `.jpeg` · `.png` · `.bmp` · `.tiff` · `.webp`

All outputs are saved as **PNG with transparency**.

---

## Pipeline

```
Load image → RGB
      │
      ▼
Dual AI mask  (birefnet-general + u2net → np.maximum)
      │
      ▼
Coverage > 85%? ──YES──▶ Trim white borders → Resize → Save PNG
      │
      NO
      │
      ▼
Fragment fill (bounding box between 2+ large fragments)
      │
      ▼
Smooth alpha (keep AI edge gradients, inner region = 255)
      │
      ▼
Apply alpha to original image
      │
      ▼
White halo removal (pure white only — HSV saturation check)
      │
      ▼
Crop with padding (max 20px or 8% of product size)
      │
      ▼
Glow effect (adaptive radius/opacity by image size)
      │
      ▼
Resize to max 1200px if needed
      │
      ▼
Save as PNG (optimize=True, compress_level=9)
```

---

## Edge Case Handling

| Product Type | Problem | Solution |
|---|---|---|
| Transparent packaging (Mentos, Haribo tubs) | AI can't separate transparent plastic from white background | Coverage > 85% → white border trim only |
| White packaging (Butterkeks, Südzucker) | White parts removed as background | Fragment fill: bounding box between detected parts |
| Silver/metallic trays (Roastbeef) | Bright metal detected as white halo | Halo removal checks HSV saturation, not just brightness |
| Dark shadows on trays | Shadows removed as background | Low threshold (> 1) for mask detection |
| Products with white outlines | AI leaves white fringe/halo | 1 px edge check for pure white + smooth |
| Multiple items on one tray | Parts detected as separate objects | Dual model + fragment fill combines them |

---

## Resume / Crash Recovery

The tool writes a `auto_crop_tracker.json` file to the output directory. Each entry maps the source filename to its MD5 hash. On restart, already-processed files are skipped automatically.

The tracker is saved every **20 images**, so a crash loses at most 20 entries of progress.

Use `--reset` to ignore the tracker and reprocess everything.

---

## Logging

Logs are written to both the console and `auto_crop.log` in the output directory. The log includes:

- Total / done / pending counts
- Per-image processing time
- Errors with filename
- Overall summary with average time per image

---

## Dependencies

| Package | Purpose |
|---|---|
| `rembg[gpu]` | AI background removal (birefnet-general, u2net) |
| `opencv-python` | Connected components, HSV conversion, Gaussian blur |
| `numpy` | Vectorized mask operations |
| `Pillow` | Image loading, saving, LANCZOS resize |
| `tqdm` | Progress bar |
