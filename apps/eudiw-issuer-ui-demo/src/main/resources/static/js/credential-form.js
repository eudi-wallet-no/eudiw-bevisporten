/**
 * credential-form.js
 *
 * Drives the add-new and edit-new credential forms.
 * State is the single source of truth; DOM is always derived from state.
 */

// ---------------------------------------------------------------------------
// Preset claims
// ---------------------------------------------------------------------------
const PRESET_CLAIMS = {
  persondata: {
    label: 'Persondata',
    claims: [
      { path: 'given_name', displayName: 'Fornavn', type: 'string', mimeType: null, exampleValue: 'Ola' },
      { path: 'family_name', displayName: 'Etternavn', type: 'string', mimeType: null, exampleValue: 'Nordmann' },
      { path: 'middle_name', displayName: 'Mellomnavn', type: 'string', mimeType: null, exampleValue: '' },
      { path: 'personal_administrative_number', displayName: 'Fødselsnummer', type: 'string', mimeType: null, exampleValue: '01018812345' },
      { path: 'birth_date', displayName: 'Fødselsdato', type: 'string', mimeType: null, exampleValue: '1988-01-01' }
    ]
  },
  kontaktinfo: {
    label: 'Kontaktinfo',
    claims: [
      { path: 'phone_number', displayName: 'Telefonnummer', type: 'string', mimeType: null, exampleValue: '+4712345678' },
      { path: 'email', displayName: 'E-post', type: 'string', mimeType: null, exampleValue: 'ola@example.no' }
    ]
  },
  bilde: {
    label: 'Bilde',
    claims: [
      {
        path: 'portrait',
        displayName: 'Profilbilde',
        type: 'binary',
        mimeType: 'image/png',
        // Minimal 1×1 gray PNG in base64
        exampleValue: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAAAAAA6fptVAAAACklEQVQI12NgAAAAAgAB4iG8MwAAAABJRU5ErkJggg=='
      }
    ]
  }
};

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------
const state = {
  mode: 'schema',       // 'schema' | 'json'
  name: '',
  credentialType: '',
  scope: '',
  claims: [],
  activePresets: new Set(),
  lastValidSchema: null
};

// ---------------------------------------------------------------------------
// Initialise
// ---------------------------------------------------------------------------
function init() {
  const dataEl = document.getElementById('credential-data');
  if (dataEl) {
    try {
      const json = JSON.parse(dataEl.textContent.trim());
      populateStateFromJson(json);
    } catch (e) {
      console.warn('credential-data parse error', e);
    }
  }
  renderAll();
  bindEvents();
}

function populateStateFromJson(json) {
  state.credentialType = json.credential_type || '';
  state.name = json.credential_metadata?.display?.[0]?.name || '';
  state.scope = json.scope || '';

  const rawClaims = json.credential_metadata?.claims || [];
  const exampleData = json.example_credential_data || {};

  state.claims = rawClaims.map(c => ({
    path: c.path || '',
    displayName: c.display?.[0]?.name || c.path || '',
    type: c.type || 'string',
    mimeType: c.mimeType || null,
    exampleValue: exampleData[c.path] !== undefined ? String(exampleData[c.path]) : '',
    presetKey: null
  }));
}

// ---------------------------------------------------------------------------
// Bind global events (live inputs that exist on page load)
// ---------------------------------------------------------------------------
function bindEvents() {
  const nameInput = document.getElementById('name-field');
  if (nameInput) {
    nameInput.addEventListener('input', () => generateIds(nameInput.value));
  }

  const credTypeInput = document.getElementById('credentialType');
  if (credTypeInput) {
    credTypeInput.addEventListener('input', () => {
      state.credentialType = credTypeInput.value;
    });
  }

  const scopeInput = document.getElementById('scope-field');
  if (scopeInput) {
    scopeInput.addEventListener('input', () => {
      state.scope = scopeInput.value;
    });
  }

  const jsonTextarea = document.getElementById('json-editor');
  if (jsonTextarea) {
    jsonTextarea.addEventListener('input', () => validateJson());
  }

  const form = document.getElementById('add_attributes_form') || document.getElementById('edit_attributes_form');
  if (form) {
    form.addEventListener('submit', onSubmit);
  }
}

// ---------------------------------------------------------------------------
// Mode switching
// ---------------------------------------------------------------------------
function switchMode(mode) {
  if (mode === state.mode) return;

  if (state.mode === 'schema') {
    // Collect any manual edits from the DOM before switching
    syncStateFromSchemaDOM();
    state.lastValidSchema = JSON.parse(JSON.stringify({ claims: state.claims, name: state.name }));
  }

  state.mode = mode;

  // Update tab aria attributes
  document.querySelectorAll('[data-mode-btn]').forEach(btn => {
    const active = btn.dataset.modeBtn === mode;
    btn.setAttribute('aria-selected', String(active));
  });

  // Show/hide panels
  const schemaPanel = document.getElementById('schema-panel');
  const jsonPanel = document.getElementById('json-panel');
  if (schemaPanel) schemaPanel.hidden = mode !== 'schema';
  if (jsonPanel) jsonPanel.hidden = mode !== 'json';

  if (mode === 'json') {
    const ta = document.getElementById('json-editor');
    if (ta) ta.value = schemaToJson();
    validateJson();
  }
}

// ---------------------------------------------------------------------------
// Schema ↔ JSON serialisation
// ---------------------------------------------------------------------------
function schemaToJson() {
  const claimsMetadata = state.claims.map(c => ({
    path: c.path,
    mandatory: false,
    display: [{ name: c.displayName, locale: 'no' }]
  }));

  const exampleData = {};
  state.claims.forEach(c => {
    if (c.exampleValue !== undefined && c.exampleValue !== null) {
      exampleData[c.path] = c.exampleValue;
    }
  });

  const out = {
    credential_type: state.credentialType,
    format: document.getElementById('format-field')?.value || 'dc+sd-jwt',
    scope: state.scope,
    credential_metadata: {
      display: [{
        name: state.name,
        locale: 'no',
        background_color: '#afcee9',
        text_color: '#002c54'
      }],
      claims: claimsMetadata
    },
    example_credential_data: exampleData
  };

  return JSON.stringify(out, null, 2);
}

function jsonToSchema(jsonStr) {
  let parsed;
  try {
    parsed = JSON.parse(jsonStr);
  } catch (e) {
    showJsonError('Ugyldig JSON – endringer er ikke lagret. ' + e.message);
    return false;
  }

  hideJsonError();
  populateStateFromJson(parsed);

  // Also sync top-level fields
  if (parsed.credential_type) {
    state.credentialType = parsed.credential_type;
    const el = document.getElementById('credentialType');
    if (el) el.value = state.credentialType;
  }
  if (parsed.scope) {
    const el = document.getElementById('scope-field');
    if (el) el.value = state.scope;
  }
  if (state.name) {
    const el = document.getElementById('name-field');
    if (el) el.value = state.name;
  }

  state.lastValidSchema = JSON.parse(JSON.stringify({ claims: state.claims, name: state.name }));
  renderClaims();
  renderPreview();
  return true;
}

// ---------------------------------------------------------------------------
// JSON validation badge
// ---------------------------------------------------------------------------
function validateJson() {
  const ta = document.getElementById('json-editor');
  const badge = document.getElementById('json-badge');
  if (!ta || !badge) return;

  try {
    JSON.parse(ta.value);
    badge.textContent = '✓ Gyldig';
    badge.className = 'json-badge json-badge--valid';
  } catch (_) {
    badge.textContent = '✗ Ugyldig JSON';
    badge.className = 'json-badge json-badge--invalid';
  }
}

function showJsonError(msg) {
  let banner = document.getElementById('json-error-banner');
  if (!banner) {
    banner = document.createElement('div');
    banner.id = 'json-error-banner';
    banner.className = 'ds-alert';
    banner.setAttribute('data-color', 'danger');
    const jsonPanel = document.getElementById('json-panel');
    if (jsonPanel) jsonPanel.prepend(banner);
  }
  banner.innerHTML = `<p class="ds-paragraph">${msg}</p>
    <button class="ds-button" data-variant="secondary" data-size="sm" type="button"
      onclick="resetJsonFromSchema()">Tilbakestill til skjema</button>`;
  banner.hidden = false;
}

function hideJsonError() {
  const banner = document.getElementById('json-error-banner');
  if (banner) banner.hidden = true;
}

function resetJsonFromSchema() {
  if (state.lastValidSchema) {
    state.claims = JSON.parse(JSON.stringify(state.lastValidSchema.claims));
    state.name = state.lastValidSchema.name;
  }
  const ta = document.getElementById('json-editor');
  if (ta) ta.value = schemaToJson();
  hideJsonError();
  validateJson();
}

// ---------------------------------------------------------------------------
// Auto-generate credentialType and scope from Bevisnavn
// ---------------------------------------------------------------------------
function generateIds(name) {
  state.name = name;
  const normalized = name
    .toLowerCase()
    .replace(/æ/g, 'ae')
    .replace(/ø/g, 'o')
    .replace(/å/g, 'a')
    .replace(/\s+/g, '_')
    .replace(/[^a-z0-9_]/g, '');

  state.credentialType = normalized ? `no.bevisgenerator.${normalized}.1` : '';
  state.scope = normalized ? `eudiw:bevisgenerator:${normalized}` : '';

  const ctEl = document.getElementById('credentialType');
  if (ctEl) ctEl.value = state.credentialType;

  const scopeEl = document.getElementById('scope-field');
  if (scopeEl) scopeEl.value = state.scope;

  renderPreview();
}

// ---------------------------------------------------------------------------
// Preset toggling
// ---------------------------------------------------------------------------
function togglePreset(key) {
  const preset = PRESET_CLAIMS[key];
  if (!preset) return;

  if (state.activePresets.has(key)) {
    // Remove claims that belong to this preset and haven't been manually edited
    state.claims = state.claims.filter(c => {
      if (c.presetKey !== key) return true;
      // Keep if manually edited (displayName or exampleValue differ from preset)
      const original = preset.claims.find(p => p.path === c.path);
      if (!original) return false;
      const edited = c.displayName !== original.displayName || c.exampleValue !== original.exampleValue;
      return edited;
    });
    state.activePresets.delete(key);
  } else {
    // Add preset claims that aren't already present
    const existingPaths = new Set(state.claims.map(c => c.path));
    preset.claims.forEach(pc => {
      if (!existingPaths.has(pc.path)) {
        state.claims.push({ ...pc, presetKey: key, mimeType: pc.mimeType || null });
      }
    });
    state.activePresets.add(key);
  }

  // Update button active state
  document.querySelectorAll('[data-preset-btn]').forEach(btn => {
    const active = state.activePresets.has(btn.dataset.presetBtn);
    btn.classList.toggle('preset-btn--active', active);
    btn.setAttribute('aria-pressed', String(active));
  });

  renderClaims();
  renderPreview();
}

// ---------------------------------------------------------------------------
// Render claims list (schema mode)
// ---------------------------------------------------------------------------
function renderClaims() {
  const container = document.getElementById('claims');
  if (!container) return;

  container.innerHTML = '';
  state.claims.forEach((claim, i) => {
    container.appendChild(buildClaimRow(claim, i));
  });
}

function buildClaimRow(claim, i) {
  const div = document.createElement('div');
  div.className = 'claim';
  div.dataset.claimIndex = i;

  // Left column: path + displayName
  const left = document.createElement('div');
  left.className = 'claim_field';

  const pathInput = document.createElement('input');
  pathInput.className = 'ds-input';
  pathInput.type = 'text';
  pathInput.name = `claims[${i}].path`;
  pathInput.value = claim.path;
  pathInput.placeholder = 'Path';
  pathInput.addEventListener('input', () => {
    state.claims[i].path = pathInput.value;
    renderPreview();
  });

  const nameInput = document.createElement('input');
  nameInput.className = 'ds-input';
  nameInput.type = 'text';
  nameInput.name = `claims[${i}].name`;
  nameInput.value = claim.displayName;
  nameInput.placeholder = 'Visningsnavn';
  nameInput.addEventListener('input', () => {
    state.claims[i].displayName = nameInput.value;
    renderPreview();
  });

  left.appendChild(pathInput);
  left.appendChild(nameInput);

  // Right column: type + mimeType (conditional) + exampleValue
  const right = document.createElement('div');
  right.className = 'claim_field';

  const typeSelect = document.createElement('select');
  typeSelect.className = 'ds-input';
  typeSelect.name = `claims[${i}].type`;
  [
    { value: '', label: 'Velg en type', disabled: true },
    { value: 'string', label: 'string' },
    { value: 'binary', label: 'binary' },
    { value: 'number', label: 'number', disabled: true },
    { value: 'boolean', label: 'boolean', disabled: true },
    { value: 'iso_date', label: 'iso_date', disabled: true },
    { value: 'iso_datetime', label: 'iso_datetime', disabled: true },
    { value: 'map', label: 'map', disabled: true },
    { value: 'list', label: 'list', disabled: true }
  ].forEach(opt => {
    const o = document.createElement('option');
    o.value = opt.value;
    o.textContent = opt.label;
    if (opt.disabled) o.disabled = true;
    if (opt.value === claim.type) o.selected = true;
    typeSelect.appendChild(o);
  });

  const mimeInput = document.createElement('input');
  mimeInput.className = 'ds-input';
  mimeInput.type = 'text';
  mimeInput.name = `claims[${i}].mimeType`;
  mimeInput.value = claim.mimeType || '';
  mimeInput.placeholder = 'MIME-type (f.eks. image/png)';
  mimeInput.hidden = claim.type !== 'binary';
  mimeInput.addEventListener('input', () => {
    state.claims[i].mimeType = mimeInput.value;
  });

  typeSelect.addEventListener('change', () => {
    state.claims[i].type = typeSelect.value;
    mimeInput.hidden = typeSelect.value !== 'binary';
    renderPreview();
  });

  const exampleInput = document.createElement('input');
  exampleInput.className = 'ds-input';
  exampleInput.type = 'text';
  exampleInput.name = `claims[${i}].exampleValue`;
  exampleInput.value = claim.exampleValue || '';
  exampleInput.placeholder = 'Eksempelverdi';
  exampleInput.addEventListener('input', () => {
    updatePreviewValue(claim.path, exampleInput.value);
  });

  right.appendChild(typeSelect);
  right.appendChild(mimeInput);
  right.appendChild(exampleInput);

  // Delete button
  const delBtn = document.createElement('button');
  delBtn.className = 'ds-button';
  delBtn.setAttribute('data-color', 'danger');
  delBtn.setAttribute('data-variant', 'tertiary');
  delBtn.type = 'button';
  delBtn.textContent = 'Slett';
  delBtn.addEventListener('click', () => {
    state.claims.splice(i, 1);
    renderClaims();
    renderPreview();
  });

  div.appendChild(left);
  div.appendChild(right);
  div.appendChild(delBtn);
  return div;
}

// ---------------------------------------------------------------------------
// Add claim (button handler, also used from HTML onclick)
// ---------------------------------------------------------------------------
function addClaim() {
  state.claims.push({ path: '', displayName: '', type: 'string', mimeType: null, exampleValue: '', presetKey: null });
  renderClaims();
  renderPreview();
  // Focus the newly added path input
  const container = document.getElementById('claims');
  if (container) {
    const last = container.lastElementChild;
    if (last) last.querySelector('input')?.focus();
  }
}

/**
 * removeClaim — compatibility shim for the server-side Thymeleaf claim fragment.
 * In add-new/edit-new this is never called (rows are JS-generated).
 * Kept here so pages that still render claims server-side don't break.
 */
function removeClaim(button) {
  const row = button.closest('.claim');
  if (!row) return;
  // If state-driven, find by index and splice
  const idx = row.dataset.claimIndex !== undefined ? Number(row.dataset.claimIndex) : -1;
  if (idx >= 0 && idx < state.claims.length) {
    state.claims.splice(idx, 1);
    renderClaims();
    renderPreview();
  } else {
    // Fallback for server-rendered rows
    row.remove();
  }
}

// ---------------------------------------------------------------------------
// Preview panel
// ---------------------------------------------------------------------------
function renderPreview() {
  const panel = document.getElementById('preview-panel');
  if (!panel) return;

  const nameEl = panel.querySelector('[data-preview-name]');
  if (nameEl) nameEl.textContent = state.name || 'Bevisnavn';

  const list = panel.querySelector('[data-preview-list]');
  if (!list) return;
  list.innerHTML = '';

  state.claims.forEach(claim => {
    if (!claim.path) return;
    const row = document.createElement('div');
    row.className = 'preview-field';

    const label = document.createElement('span');
    label.className = 'preview-label';
    label.textContent = claim.displayName || claim.path;

    const val = document.createElement('span');
    val.className = 'preview-value';
    val.contentEditable = 'true';
    val.spellcheck = false;
    val.textContent = claim.type === 'binary'
      ? '[bilde]'
      : (claim.exampleValue || '—');
    val.addEventListener('input', () => {
      updatePreviewValue(claim.path, val.textContent);
    });

    row.appendChild(label);
    row.appendChild(val);
    list.appendChild(row);
  });
}

function updatePreviewValue(path, newValue) {
  const claim = state.claims.find(c => c.path === path);
  if (claim) {
    claim.exampleValue = newValue;
    // Sync to JSON textarea if in json mode
    if (state.mode === 'json') {
      const ta = document.getElementById('json-editor');
      if (ta) {
        try {
          const parsed = JSON.parse(ta.value);
          if (parsed.example_credential_data) {
            parsed.example_credential_data[path] = newValue;
            ta.value = JSON.stringify(parsed, null, 2);
          }
        } catch (_) { /* leave textarea as-is if invalid */ }
      }
    }
    // Also sync schema input if visible
    const container = document.getElementById('claims');
    if (container) {
      const claimIdx = state.claims.indexOf(claim);
      const row = container.querySelector(`[data-claim-index="${claimIdx}"]`);
      if (row) {
        const exInput = row.querySelector(`input[name="claims[${claimIdx}].exampleValue"]`);
        if (exInput && exInput.value !== newValue) exInput.value = newValue;
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Sync state from schema DOM (before switching to JSON mode)
// ---------------------------------------------------------------------------
function syncStateFromSchemaDOM() {
  const container = document.getElementById('claims');
  if (!container) return;

  const nameInput = document.getElementById('name-field');
  if (nameInput) state.name = nameInput.value;

  const ctInput = document.getElementById('credentialType');
  if (ctInput) state.credentialType = ctInput.value;

  const scopeInput = document.getElementById('scope-field');
  if (scopeInput) state.scope = scopeInput.value;

  // Claims are kept in sync via event listeners on inputs; no extra work needed
}

// ---------------------------------------------------------------------------
// Form submit
// ---------------------------------------------------------------------------
function onSubmit(event) {
  const rawJsonInput = document.getElementById('rawJson-input');

  if (state.mode === 'json') {
    const ta = document.getElementById('json-editor');
    const jsonStr = ta ? ta.value : '';
    try {
      JSON.parse(jsonStr);
    } catch (e) {
      event.preventDefault();
      showJsonError('Skjemaet kan ikke sendes inn – JSON er ugyldig: ' + e.message);
      return;
    }
    if (rawJsonInput) rawJsonInput.value = jsonStr;

    // Disable schema inputs so they don't interfere with form data
    const container = document.getElementById('claims');
    if (container) {
      container.querySelectorAll('input, select, textarea').forEach(el => {
        el.disabled = true;
      });
    }
  } else {
    // schema mode — ensure rawJson is empty
    if (rawJsonInput) rawJsonInput.value = '';
  }
}

// ---------------------------------------------------------------------------
// Render everything
// ---------------------------------------------------------------------------
function renderAll() {
  // Populate top-level inputs from state (useful in edit mode)
  const nameInput = document.getElementById('name-field');
  if (nameInput && state.name) nameInput.value = state.name;

  const ctInput = document.getElementById('credentialType');
  if (ctInput && state.credentialType) ctInput.value = state.credentialType;

  const scopeInput = document.getElementById('scope-field');
  if (scopeInput && state.scope) scopeInput.value = state.scope;

  renderClaims();
  renderPreview();
}

// ---------------------------------------------------------------------------
// Boot
// ---------------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', init);