/* ============================================================
   admin.js — Admin Frontend Logic
   ============================================================ */

const API = '/api/admin/wines';

let editingId = null;

// ---- DOM References ----
const wineTableBody  = document.getElementById('wineTableBody');
const formModal      = document.getElementById('formModal');
const wineForm       = document.getElementById('wineForm');
const formTitle      = document.getElementById('formTitle');
const addWineBtn     = document.getElementById('addWineBtn');
const closeFormModal = document.getElementById('closeFormModal');
const cancelForm     = document.getElementById('cancelForm');
const toastContainer = document.getElementById('toastContainer');

// ---- Field IDs ----
const FIELDS = ['name','winery','country','region','grapeVariety','wineType',
                'sweetness','body','acidity','tannin','price','rating',
                'flavorNotes','foodPairing','imageUrl','description'];

// ---- Init ----
window.addEventListener('DOMContentLoaded', loadWines);

// ---- Load wines ----
async function loadWines() {
  try {
    const res = await fetch(API);
    if (!res.ok) throw new Error('Failed to load wines');
    const wines = await res.json();
    renderTable(wines);
  } catch (e) {
    wineTableBody.innerHTML = `<tr><td colspan="10" style="color:var(--error);text-align:center;padding:20px;">Error loading wines: ${escapeHtml(e.message)}</td></tr>`;
    showToast('Failed to load wines', 'error');
  }
}

// ---- Render Table ----
function renderTable(wines) {
  if (wines.length === 0) {
    wineTableBody.innerHTML = `<tr><td colspan="10" style="text-align:center;color:var(--text-muted);padding:30px;">No wines found. Add your first wine!</td></tr>`;
    return;
  }
  wineTableBody.innerHTML = wines.map(w => `
    <tr>
      <td style="color:var(--text-muted);">#${w.id}</td>
      <td><strong style="color:var(--text-primary);">${escapeHtml(w.name)}</strong></td>
      <td>${escapeHtml(w.winery || '—')}</td>
      <td>
        <span style="display:inline-flex;align-items:center;gap:5px;">
          <span style="width:8px;height:8px;border-radius:50%;background:${wineTypeColor(w.wineType)};display:inline-block;"></span>
          ${escapeHtml(w.wineType || '—')}
        </span>
      </td>
      <td>${escapeHtml(w.region || '—')}, ${escapeHtml(w.country || '—')}</td>
      <td>${escapeHtml(w.grapeVariety || '—')}</td>
      <td>${escapeHtml(w.sweetness || '—')}</td>
      <td style="color:var(--gold-300);font-weight:700;">€${parseFloat(w.price || 0).toFixed(2)}</td>
      <td>⭐ ${w.rating || '—'}</td>
      <td>
        <button class="btn btn-sm btn-edit" data-id="${w.id}">✏ Edit</button>
        <button class="btn btn-sm btn-delete" data-id="${w.id}" data-name="${escapeHtml(w.name)}" style="margin-left:4px;">🗑 Delete</button>
      </td>
    </tr>
  `).join('');

  // Attach event listeners to avoid inline onclick
  wineTableBody.querySelectorAll('.btn-edit').forEach(btn => {
    btn.addEventListener('click', () => editWine(parseInt(btn.dataset.id, 10)));
  });
  wineTableBody.querySelectorAll('.btn-delete').forEach(btn => {
    btn.addEventListener('click', () => deleteWine(parseInt(btn.dataset.id, 10), btn.dataset.name));
  });
}

// ---- Open Add Form ----
addWineBtn.addEventListener('click', () => openForm(null));

function openForm(wine) {
  editingId = wine ? wine.id : null;
  formTitle.textContent = wine ? '✏ Edit Wine' : '+ Add Wine';
  clearForm();
  if (wine) fillForm(wine);
  formModal.classList.add('open');
}

function clearForm() {
  FIELDS.forEach(f => {
    const el = document.getElementById('f-' + f);
    if (el) el.value = '';
  });
}

function fillForm(wine) {
  const map = {
    name: wine.name, winery: wine.winery, country: wine.country,
    region: wine.region, grapeVariety: wine.grapeVariety, wineType: wine.wineType,
    sweetness: wine.sweetness, body: wine.body, acidity: wine.acidity,
    tannin: wine.tannin, price: wine.price, rating: wine.rating,
    flavorNotes: wine.flavorNotes, foodPairing: wine.foodPairing,
    imageUrl: wine.imageUrl, description: wine.description
  };
  Object.entries(map).forEach(([k, v]) => {
    const el = document.getElementById('f-' + k);
    if (el && v !== null && v !== undefined) el.value = v;
  });
}

// ---- Edit Wine ----
async function editWine(id) {
  try {
    const res = await fetch(`/api/wines/${id}`);
    if (!res.ok) throw new Error('Wine not found');
    const wine = await res.json();
    openForm(wine);
  } catch (e) {
    showToast('Failed to load wine details', 'error');
  }
}

// ---- Delete Wine ----
async function deleteWine(id, name) {
  if (!confirm(`Delete "${name}"? This cannot be undone.`)) return;
  try {
    const res = await fetch(`${API}/${id}`, { method: 'DELETE' });
    if (res.status === 204 || res.ok) {
      showToast(`"${name}" deleted successfully`, 'success');
      loadWines();
    } else {
      throw new Error('Delete failed');
    }
  } catch (e) {
    showToast('Failed to delete wine', 'error');
  }
}

// ---- Form Submit ----
wineForm.addEventListener('submit', async e => {
  e.preventDefault();
  const saveBtn = document.getElementById('saveWineBtn');
  saveBtn.disabled = true;
  saveBtn.textContent = 'Saving…';

  try {
    const wine = buildWineFromForm();
    const url    = editingId ? `${API}/${editingId}` : API;
    const method = editingId ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(wine)
    });

    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    showToast(editingId ? 'Wine updated!' : 'Wine added!', 'success');
    closeForm();
    loadWines();
  } catch (e) {
    showToast('Failed to save wine: ' + e.message, 'error');
  } finally {
    saveBtn.disabled = false;
    saveBtn.textContent = '💾 Save Wine';
  }
});

function buildWineFromForm() {
  return {
    name:         val('name'),
    winery:       val('winery'),
    country:      val('country'),
    region:       val('region'),
    grapeVariety: val('grapeVariety'),
    wineType:     val('wineType'),
    sweetness:    val('sweetness'),
    body:         val('body'),
    acidity:      val('acidity'),
    tannin:       val('tannin'),
    price:        parseFloat(val('price')) || 0,
    rating:       val('rating') ? parseFloat(val('rating')) : null,
    flavorNotes:  val('flavorNotes'),
    foodPairing:  val('foodPairing'),
    imageUrl:     val('imageUrl'),
    description:  val('description')
  };
}

function val(field) {
  const el = document.getElementById('f-' + field);
  return el ? el.value.trim() || null : null;
}

// ---- Close Form ----
function closeForm() {
  formModal.classList.remove('open');
  clearForm();
  editingId = null;
}

closeFormModal.addEventListener('click', closeForm);
cancelForm.addEventListener('click', closeForm);
formModal.addEventListener('click', e => { if (e.target === formModal) closeForm(); });

// ---- Toast ----
function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <span>${type === 'success' ? '✅' : '❌'}</span>
    <span>${escapeHtml(message)}</span>
  `;
  toastContainer.appendChild(toast);
  setTimeout(() => toast.remove(), 4000);
}

// ---- Utility ----
function wineTypeColor(type) {
  if (!type) return '#888';
  const t = type.toLowerCase();
  if (t === 'white') return '#d4a843';
  if (t.includes('ros')) return '#db7093';
  return '#9b1c1c';
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
