/* ============================================================
   app.js — Customer Kiosk Frontend Logic
   ============================================================ */

const API = '/api';

// ---- State ----
let allWines = [];
let filteredWines = [];
let filterOptions = {};

// ---- DOM References ----
const chatMessages   = document.getElementById('chatMessages');
const chatInput      = document.getElementById('chatInput');
const sendBtn        = document.getElementById('sendBtn');
const recommendedDiv = document.getElementById('recommendedWines');
const wineGrid       = document.getElementById('wineGrid');
const wineCount      = document.getElementById('wineCount');
const wineModal      = document.getElementById('wineModal');
const closeModal     = document.getElementById('closeModal');
const modalHeader    = document.getElementById('modalHeader');
const modalBody      = document.getElementById('modalBody');

// ---- Tab Switching ----
document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    const tab = btn.dataset.tab;
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('tab-' + tab).classList.add('active');
    if (tab === 'browse' && allWines.length === 0) {
      loadBrowseTab();
    }
  });
});

// ---- Welcome Message ----
window.addEventListener('DOMContentLoaded', () => {
  appendAIMessage(
    'Welcome to **Vinothek AI** — your personal wine sommelier! 🍷\n\n' +
    'I can help you find the perfect wine from our selection. Just tell me what you\'re looking for — ' +
    'a specific style, food pairing, occasion, or budget — and I\'ll recommend wines from our inventory.\n\n' +
    'What can I help you find today?'
  );
  loadFilterOptions();
});

// ---- Chat ----
sendBtn.addEventListener('click', sendMessage);
chatInput.addEventListener('keydown', e => { if (e.key === 'Enter') sendMessage(); });

document.querySelectorAll('.suggestion-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    chatInput.value = btn.dataset.msg;
    sendMessage();
  });
});

async function sendMessage() {
  const text = chatInput.value.trim();
  if (!text) return;

  chatInput.value = '';
  appendUserMessage(text);
  setLoading(true);

  try {
    const res = await fetch(`${API}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text })
    });

    if (!res.ok) throw new Error('Network response was not ok');
    const data = await res.json();

    appendAIMessage(data.reply);
    renderRecommendedWines(data.recommendedWines || []);
  } catch (err) {
    appendAIMessage('Sorry, something went wrong. Please try again. 😔');
    console.error('Chat error:', err);
  } finally {
    setLoading(false);
  }
}

function appendUserMessage(text) {
  const div = document.createElement('div');
  div.className = 'chat-message user';
  div.innerHTML = `
    <div class="chat-avatar">👤</div>
    <div class="chat-bubble">${escapeHtml(text)}</div>
  `;
  chatMessages.appendChild(div);
  scrollToBottom();
}

function appendAIMessage(text) {
  const div = document.createElement('div');
  div.className = 'chat-message ai';
  div.innerHTML = `
    <div class="chat-avatar">🍷</div>
    <div class="chat-bubble">${renderMarkdown(text)}</div>
  `;
  chatMessages.appendChild(div);
  scrollToBottom();
}

let loadingEl = null;

function setLoading(on) {
  sendBtn.disabled = on;
  if (on) {
    loadingEl = document.createElement('div');
    loadingEl.className = 'chat-message ai typing-indicator';
    loadingEl.innerHTML = `
      <div class="chat-avatar">🍷</div>
      <div class="typing-dots"><span></span><span></span><span></span></div>
      <span style="font-size:0.8rem;color:var(--text-muted);">Sommelier is thinking…</span>
    `;
    chatMessages.appendChild(loadingEl);
    scrollToBottom();
  } else if (loadingEl) {
    loadingEl.remove();
    loadingEl = null;
  }
}

function scrollToBottom() {
  chatMessages.scrollTop = chatMessages.scrollHeight;
}

// ---- Recommended Wines Sidebar ----
function renderRecommendedWines(wines) {
  if (!wines || wines.length === 0) return;
  recommendedDiv.innerHTML = '';
  wines.forEach(wine => {
    const card = document.createElement('div');
    card.className = 'wine-card-mini';
    card.innerHTML = `
      <div class="wine-card-mini-header">
        <div class="wine-type-dot ${wineTypeClass(wine.wineType)}"></div>
        <span class="wine-card-mini-name">${escapeHtml(wine.name)}</span>
      </div>
      <div class="wine-card-mini-winery">${escapeHtml(wine.winery || '')} · ${escapeHtml(wine.region || '')}, ${escapeHtml(wine.country || '')}</div>
      <div class="wine-card-mini-meta">
        <span class="wine-card-mini-price">€${parseFloat(wine.price).toFixed(2)}</span>
        <span class="wine-card-mini-rating">⭐ ${wine.rating || '—'}</span>
      </div>
    `;
    card.addEventListener('click', () => openWineModal(wine));
    recommendedDiv.appendChild(card);
  });
}

// ---- Browse Tab ----
async function loadBrowseTab() {
  await loadWines();
}

async function loadFilterOptions() {
  try {
    const res = await fetch(`${API}/wines/filters`);
    filterOptions = await res.json();
    populateFilterDropdowns();
  } catch (e) {
    console.error('Could not load filter options', e);
  }
}

function populateFilterDropdowns() {
  populateSelect('filterCountry', filterOptions.countries || [], 'All Countries');
  populateSelect('filterRegion', filterOptions.regions || [], 'All Regions');
  populateSelect('filterGrape', filterOptions.grapeVarieties || [], 'All Grapes');
}

function populateSelect(id, values, placeholder) {
  const sel = document.getElementById(id);
  sel.innerHTML = `<option value="">${placeholder}</option>`;
  values.forEach(v => {
    const opt = document.createElement('option');
    opt.value = v;
    opt.textContent = v;
    sel.appendChild(opt);
  });
}

async function loadWines() {
  const body = buildFilterRequest();
  try {
    const res = await fetch(`${API}/wines/filter`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    filteredWines = await res.json();
    renderWineGrid(filteredWines);
  } catch (e) {
    console.error('Failed to load wines', e);
    wineGrid.innerHTML = '<p style="color:var(--text-muted);padding:20px;">Failed to load wines.</p>';
  }
}

function buildFilterRequest() {
  const req = {};
  const type     = document.getElementById('filterType').value;
  const country  = document.getElementById('filterCountry').value;
  const region   = document.getElementById('filterRegion').value;
  const grape    = document.getElementById('filterGrape').value;
  const sweet    = document.getElementById('filterSweetness').value;
  const maxPrice = document.getElementById('filterMaxPrice').value;
  const search   = document.getElementById('filterSearch').value.trim();

  if (type)     req.wineType     = type;
  if (country)  req.country      = country;
  if (region)   req.region       = region;
  if (grape)    req.grapeVariety = grape;
  if (sweet)    req.sweetness    = sweet;
  if (maxPrice) req.maxPrice     = parseFloat(maxPrice);
  if (search)   req.searchText   = search;

  return req;
}

// Add filter change listeners
['filterType','filterCountry','filterRegion','filterGrape','filterSweetness','filterMaxPrice','filterSearch'].forEach(id => {
  const el = document.getElementById(id);
  if (el) {
    el.addEventListener('change', loadWines);
    if (el.tagName === 'INPUT') el.addEventListener('input', debounce(loadWines, 400));
  }
});

document.getElementById('resetFilters').addEventListener('click', () => {
  ['filterType','filterCountry','filterRegion','filterGrape','filterSweetness'].forEach(id => {
    document.getElementById(id).value = '';
  });
  document.getElementById('filterMaxPrice').value = '';
  document.getElementById('filterSearch').value = '';
  loadWines();
});

function renderWineGrid(wines) {
  wineCount.textContent = `${wines.length} wine${wines.length !== 1 ? 's' : ''} found`;
  if (wines.length === 0) {
    wineGrid.innerHTML = `
      <div class="empty-state" style="grid-column:1/-1">
        <div class="icon">🍾</div>
        <p>No wines match your filters. Try adjusting the criteria.</p>
      </div>`;
    return;
  }
  wineGrid.innerHTML = '';
  wines.forEach(wine => {
    const card = createWineCard(wine);
    wineGrid.appendChild(card);
  });
}

function createWineCard(wine) {
  const card = document.createElement('div');
  card.className = `wine-card ${wineTypeClass(wine.wineType)}-wine`;

  const stars = ratingStars(wine.rating);
  card.innerHTML = `
    <div class="wine-card-header">
      <div class="wine-type-dot ${wineTypeClass(wine.wineType)}" style="margin-top:4px;"></div>
      <span class="wine-card-name">${escapeHtml(wine.name)}</span>
    </div>
    <div class="wine-card-winery">${escapeHtml(wine.winery || '')}</div>
    <div class="wine-card-location">📍 ${escapeHtml(wine.region || '')}${wine.region && wine.country ? ', ' : ''}${escapeHtml(wine.country || '')}</div>
    <div class="badge-row">
      ${wine.grapeVariety ? `<span class="badge badge-grape">🍇 ${escapeHtml(wine.grapeVariety)}</span>` : ''}
      ${wine.sweetness   ? `<span class="badge badge-sweet">${escapeHtml(wine.sweetness)}</span>` : ''}
      ${wine.body        ? `<span class="badge badge-body">${escapeHtml(wine.body)}</span>` : ''}
    </div>
    ${wine.flavorNotes ? `<div class="wine-card-flavors">🌿 ${escapeHtml(wine.flavorNotes)}</div>` : ''}
    ${wine.foodPairing ? `<div class="wine-card-food">🍽 ${escapeHtml(wine.foodPairing)}</div>` : ''}
    <div class="wine-card-footer">
      <span class="wine-price">€${parseFloat(wine.price).toFixed(2)}</span>
      <span class="wine-rating">${stars} ${wine.rating || ''}</span>
    </div>
  `;
  card.addEventListener('click', () => openWineModal(wine));
  return card;
}

// ---- Wine Detail Modal ----
function openWineModal(wine) {
  modalHeader.innerHTML = `
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:6px;">
      <div class="wine-type-dot ${wineTypeClass(wine.wineType)}" style="width:14px;height:14px;"></div>
      <h2 style="font-family:'Playfair Display',serif;font-size:1.4rem;">${escapeHtml(wine.name)}</h2>
    </div>
    <p style="color:var(--text-secondary);font-size:0.9rem;">${escapeHtml(wine.winery || '')}</p>
  `;

  modalBody.innerHTML = `
    <div class="detail-grid">
      <div class="detail-item"><label>Type</label><span>${escapeHtml(wine.wineType || '—')}</span></div>
      <div class="detail-item"><label>Country</label><span>${escapeHtml(wine.country || '—')}</span></div>
      <div class="detail-item"><label>Region</label><span>${escapeHtml(wine.region || '—')}</span></div>
      <div class="detail-item"><label>Grape Variety</label><span>${escapeHtml(wine.grapeVariety || '—')}</span></div>
      <div class="detail-item"><label>Sweetness</label><span>${escapeHtml(wine.sweetness || '—')}</span></div>
      <div class="detail-item"><label>Body</label><span>${escapeHtml(wine.body || '—')}</span></div>
      <div class="detail-item"><label>Acidity</label><span>${escapeHtml(wine.acidity || '—')}</span></div>
      <div class="detail-item"><label>Tannin</label><span>${escapeHtml(wine.tannin || '—')}</span></div>
      <div class="detail-item"><label>Price</label><span class="detail-price">€${parseFloat(wine.price || 0).toFixed(2)}</span></div>
      <div class="detail-item"><label>Rating</label><span>${ratingStars(wine.rating)} ${wine.rating || '—'}</span></div>
    </div>
    ${wine.flavorNotes ? `
      <div style="margin-bottom:12px;">
        <p style="font-size:0.75rem;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.5px;margin-bottom:4px;">Flavor Notes</p>
        <p style="font-size:0.9rem;color:var(--text-secondary);">🌿 ${escapeHtml(wine.flavorNotes)}</p>
      </div>` : ''}
    ${wine.foodPairing ? `
      <div style="margin-bottom:12px;">
        <p style="font-size:0.75rem;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.5px;margin-bottom:4px;">Food Pairing</p>
        <p style="font-size:0.9rem;color:var(--text-secondary);">🍽 ${escapeHtml(wine.foodPairing)}</p>
      </div>` : ''}
    ${wine.description ? `
      <div>
        <p style="font-size:0.75rem;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.5px;margin-bottom:4px;">Description</p>
        <p style="font-size:0.9rem;color:var(--text-secondary);line-height:1.6;">${escapeHtml(wine.description)}</p>
      </div>` : ''}
  `;

  wineModal.classList.add('open');
}

closeModal.addEventListener('click', () => wineModal.classList.remove('open'));
wineModal.addEventListener('click', e => {
  if (e.target === wineModal) wineModal.classList.remove('open');
});

// ---- Utility Functions ----
function wineTypeClass(type) {
  if (!type) return 'red';
  const t = type.toLowerCase();
  if (t === 'white') return 'white';
  if (t.includes('ros')) return 'rose';
  return 'red';
}

function ratingStars(rating) {
  if (!rating) return '☆☆☆☆☆';
  const r = parseFloat(rating);
  const full = Math.floor(r);
  const half = r - full >= 0.5;
  let stars = '★'.repeat(full);
  if (half) stars += '½';
  return stars || '☆';
}

function renderMarkdown(text) {
  if (!text) return '';
  // Bold
  let html = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  // Numbered list
  html = html.replace(/^\d+\.\s+(.+)$/gm, '<li>$1</li>');
  html = html.replace(/(<li>.*<\/li>)/s, '<ol>$1</ol>');
  // Line breaks
  html = html.replace(/\n/g, '<br>');
  return html;
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

function debounce(fn, delay) {
  let timer;
  return function(...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}
