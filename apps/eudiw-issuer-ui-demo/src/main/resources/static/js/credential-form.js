/**
 * credential-form.js
 *
 * Drives the add-new and edit-new credential forms.
 * State is the single source of truth; DOM is always derived from state.
 *
 * PRESET_CLAIMS is defined in preset-claims.js, loaded before this file.
 */

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
  lastValidSchema: null,
  backgroundColor: '#ffffff',
  textColor: '#000000'
};

// ---------------------------------------------------------------------------
// Initialise
// ---------------------------------------------------------------------------
let initialState = null;

function captureInitialState() {
  initialState = {
    name: state.name,
    credentialType: state.credentialType,
    scope: state.scope,
    claims: JSON.parse(JSON.stringify(state.claims)),
    activePresets: new Set(state.activePresets),
    backgroundColor: state.backgroundColor,
    textColor: state.textColor
  };
}

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
  captureInitialState();
  renderPresetButtons();
  renderAll();
  bindEvents();
}

function resetCredential() {
  if (!initialState) return;

  state.name = initialState.name;
  state.credentialType = initialState.credentialType;
  state.scope = initialState.scope;
  state.claims = JSON.parse(JSON.stringify(initialState.claims));
  state.activePresets = new Set(initialState.activePresets);
  state.backgroundColor = initialState.backgroundColor;
  state.textColor = initialState.textColor;

  syncTopLevelInputsFromState();

  const bgInput = document.getElementById('bg-color-input');
  if (bgInput) bgInput.value = state.backgroundColor;

  const textInput = document.getElementById('text-color-input');
  if (textInput) textInput.value = state.textColor;

  hideJsonError();
  const customClaimError = document.getElementById('custom-claim-error');
  if (customClaimError) customClaimError.hidden = true;

  renderClaims();
  renderPreview();
  syncPresetButtons();
  syncJsonTextarea();
}

function populateStateFromJson(json) {
  state.credentialType = json.credential_type || '';
  state.name = json.credential_metadata?.display?.[0]?.name || '';
  state.scope = json.scope || '';
  
  // Extract colors from display[0]
  state.backgroundColor = json.credential_metadata?.display?.[0]?.background_color || '#ffffff';
  state.textColor = json.credential_metadata?.display?.[0]?.text_color || '#000000';

  const rawClaims = json.credential_metadata?.claims || [];
  const exampleData = json.example_credential_data || {};

  state.claims = rawClaims.map(c => ({
    path: c.path || '',
    displayName: c.display?.[0]?.name || c.path || '',
    type: c.value_type || 'string',
    mimeType: c.mime_type || null,
    exampleValue: exampleData[c.path] !== undefined ? String(exampleData[c.path]) : '',
    presetKey: detectPresetKey(c.path)
  }));

  // Sync activePresets from detected claims
  state.activePresets.clear();
  state.claims.forEach(c => {
    if (c.presetKey) {
      const preset = PRESET_CLAIMS[c.presetKey];
      // Only mark preset active if all its paths are present
      const allPresent = preset.claims.every(p => state.claims.some(sc => sc.path === p.path));
      if (allPresent) state.activePresets.add(c.presetKey);
    }
  });
}

function detectPresetKey(path) {
  for (const [key, preset] of Object.entries(PRESET_CLAIMS)) {
    if (preset.claims.some(p => p.path === path)) return key;
  }
  return null;
}

function syncJsonTextarea() {
  if (state.mode === 'json') {
    const ta = document.getElementById('json-editor');
    if (ta) ta.value = schemaToJson();
    validateJson();
  }
}

// ---------------------------------------------------------------------------
// Bind global events (live inputs that exist on page load)
// ---------------------------------------------------------------------------
function bindEvents() {
  const nameInput = document.getElementById('name-field');
  if (nameInput) {
    nameInput.addEventListener('input', () => {
      generateIds(nameInput.value);
      if (state.mode === 'json') {
        const ta = document.getElementById('json-editor');
        if (ta) {
          ta.value = schemaToJson();
          validateJson();
        }
      }
    });
  }

  const credTypeInput = document.getElementById('credentialType');
  if (credTypeInput) {
    credTypeInput.addEventListener('input', () => {
      state.credentialType = credTypeInput.value;
      if (state.mode === 'json') syncJsonTextarea();
    });
  }

  const scopeInput = document.getElementById('scope-field');
  if (scopeInput) {
    scopeInput.addEventListener('input', () => {
      state.scope = scopeInput.value;
      if (state.mode === 'json') syncJsonTextarea();
    });
  }

  const jsonTextarea = document.getElementById('json-editor');
  if (jsonTextarea) {
    let jsonDebounce = null;
    jsonTextarea.addEventListener('input', () => {
      validateJson();
      clearTimeout(jsonDebounce);
      jsonDebounce = setTimeout(() => {
        jsonToSchema(jsonTextarea.value);
        schedulePreviewUpdate();
      }, 500);
    });
  }

  // Color controls
  const bgColorInput = document.getElementById('bg-color-input');
  if (bgColorInput) {
    bgColorInput.addEventListener('input', () => {
      state.backgroundColor = bgColorInput.value;
      if (state.mode === 'json') {
        syncJsonTextarea();
      }
      schedulePreviewUpdate();
    });
  }

  const textColorInput = document.getElementById('text-color-input');
  if (textColorInput) {
    textColorInput.addEventListener('input', () => {
      state.textColor = textColorInput.value;
      if (state.mode === 'json') {
        syncJsonTextarea();
      }
      schedulePreviewUpdate();
    });
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

  // Hide spinner and cancel any pending preview update
  clearTimeout(previewTimer);
  const spinner = document.getElementById('preview-spinner');
  if (spinner) spinner.hidden = true;

  if (state.mode === 'schema') {
    syncStateFromSchemaDOM();
    state.lastValidSchema = JSON.parse(JSON.stringify({ claims: state.claims, name: state.name }));
  } else {
    // Leaving JSON mode: parse current textarea into state
    const ta = document.getElementById('json-editor');
    if (ta) jsonToSchema(ta.value);
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
    // Sync color inputs
    const bgInput = document.getElementById('bg-color-input');
    const textInput = document.getElementById('text-color-input');
    if (bgInput) bgInput.value = state.backgroundColor;
    if (textInput) textInput.value = state.textColor;
  } else {
    renderClaims();
    renderPreview();
    syncTopLevelInputsFromState();
  }
}

function syncTopLevelInputsFromState() {
  const nameEl = document.getElementById('name-field');
  if (nameEl) nameEl.value = state.name;

  const ctEl = document.getElementById('credentialType');
  if (ctEl) ctEl.value = state.credentialType;

  const scopeEl = document.getElementById('scope-field');
  if (scopeEl) scopeEl.value = state.scope;

  // Sync preset button states
  syncPresetButtons();
}

// ---------------------------------------------------------------------------
// Schema ↔ JSON serialisation
// ---------------------------------------------------------------------------
function schemaToJson() {
  const claimsMetadata = state.claims.map(c => ({
    path: c.path,
    value_type: c.type || 'string',
    mandatory: false,
    display: [{ name: c.displayName, locale: 'no' }],
    mime_type: c.mimeType || null
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
        background_color: state.backgroundColor,
        text_color: state.textColor
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

  // Sync top-level fields to DOM after state is updated
  syncTopLevelInputsFromState();
  
  // Sync color inputs from state
  const bgColorInput = document.getElementById('bg-color-input');
  if (bgColorInput) bgColorInput.value = state.backgroundColor;
  
  const textColorInput = document.getElementById('text-color-input');
  if (textColorInput) textColorInput.value = state.textColor;

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

  schedulePreviewUpdate();
}

// ---------------------------------------------------------------------------
// Preset toggling
// ---------------------------------------------------------------------------
function renderPresetButtons() {
  const container = document.getElementById('preset-buttons');
  if (!container) return;

  container.innerHTML = '';
  Object.keys(PRESET_CLAIMS).forEach(key => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'ds-button';
    btn.setAttribute('data-color', 'accent');
    btn.setAttribute('data-variant', 'secondary');
    btn.setAttribute('data-preset-btn', key);
    btn.setAttribute('aria-pressed', 'false');
    btn.addEventListener('click', () => togglePreset(key));
    container.appendChild(btn);
  });

  const customBtn = document.createElement('button');
  customBtn.type = 'button';
  customBtn.className = 'ds-button';
  customBtn.setAttribute('data-color', 'neutral');
  customBtn.setAttribute('data-variant', 'secondary');
  customBtn.setAttribute('aria-label', 'Legg til eget claim');
  customBtn.textContent = '+ Legg til eget claim';
  customBtn.addEventListener('click', openCustomClaimModal);
  container.appendChild(customBtn);

  syncPresetButtons();
}

function togglePreset(key) {
  const preset = PRESET_CLAIMS[key];
  if (!preset) return;

  if (state.activePresets.has(key)) {
    // Remove all claims that belong to this preset, regardless of edits
    state.claims = state.claims.filter(c => c.presetKey !== key);
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
  syncPresetButtons();

  renderClaims();
  renderPreview();
  syncJsonTextarea();
}

function syncPresetButtons() {
  document.querySelectorAll('[data-preset-btn]').forEach(btn => {
    const active = state.activePresets.has(btn.dataset.presetBtn);
    btn.setAttribute('aria-pressed', String(active));
    btn.setAttribute('data-variant', active ? 'primary' : 'secondary');
    const label = PRESET_CLAIMS[btn.dataset.presetBtn]?.label || btn.dataset.presetBtn;
    btn.textContent = (active ? '✓ ' : '+ ') + label;
  });
}

// ---------------------------------------------------------------------------
// Custom claim modal
// ---------------------------------------------------------------------------
let customClaimModalTrigger = null;

function openCustomClaimModal() {
  const modal = document.getElementById('custom-claim-modal');
  const nameInput = document.getElementById('custom-claim-name');
  const valueInput = document.getElementById('custom-claim-value');
  const errorDiv = document.getElementById('custom-claim-error');

  if (modal && nameInput && valueInput && errorDiv) {
    // Store reference to the button that opened the modal
    customClaimModalTrigger = document.activeElement;
    
    nameInput.value = '';
    valueInput.value = '';
    errorDiv.hidden = true;
    modal.showModal();
    nameInput.focus();
  }
}

function closeCustomClaimModal() {
  const modal = document.getElementById('custom-claim-modal');
  if (modal) {
    modal.close();
    // Return focus to the button that opened the modal
    if (customClaimModalTrigger && customClaimModalTrigger.focus) {
      customClaimModalTrigger.focus();
    }
  }
}

function submitCustomClaim() {
  const nameInput = document.getElementById('custom-claim-name');
  const valueInput = document.getElementById('custom-claim-value');
  const errorDiv = document.getElementById('custom-claim-error');

  if (!nameInput || !valueInput || !errorDiv) return;

  const name = nameInput.value.trim();
  const value = valueInput.value.trim();

  if (!name) {
    showCustomClaimError(errorDiv, 'Claim navn er påkrevd');
    return;
  }

  // Generate path from name (same logic as credentialType)
  const path = name.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_]/g, '');

  if (!path) {
    showCustomClaimError(errorDiv, 'Claim namn må innehalde bokstavar eller tal');
    return;
  }

  // Check for duplicate path
  if (state.claims.some(c => c.path === path)) {
    showCustomClaimError(errorDiv, `Claim med path "${path}" finst allereie`);
    return;
  }

  // Add the claim
  state.claims.push({
    path,
    displayName: name,
    type: 'string',
    mimeType: null,
    exampleValue: value,
    presetKey: null
  });

  renderClaims();
  renderPreview();
  syncJsonTextarea();
  closeCustomClaimModal();
}

function showCustomClaimError(errorDiv, message) {
  errorDiv.textContent = message;
  errorDiv.hidden = false;
}

// ---------------------------------------------------------------------------
// Render claims list (schema mode — preset claims with locked name/type,
// plus any custom claims parsed from JSON shown read-only)
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

  const label = document.createElement('span');
  label.className = 'claim-preset-label';
  label.textContent = claim.displayName || claim.path;

  // Hidden inputs so Spring MVC gets the full claim on submit
  const pathHidden = document.createElement('input');
  pathHidden.type = 'hidden';
  pathHidden.name = `claims[${i}].path`;
  pathHidden.value = claim.path;

  const nameHidden = document.createElement('input');
  nameHidden.type = 'hidden';
  nameHidden.name = `claims[${i}].name`;
  nameHidden.value = claim.displayName;

  const typeHidden = document.createElement('input');
  typeHidden.type = 'hidden';
  typeHidden.name = `claims[${i}].type`;
  typeHidden.value = claim.type || 'string';

  const mimeHidden = document.createElement('input');
  mimeHidden.type = 'hidden';
  mimeHidden.name = `claims[${i}].mimeType`;
  mimeHidden.value = claim.mimeType || '';

  const exampleInput = document.createElement('input');
  exampleInput.className = 'ds-input claim-preset-example';
  exampleInput.type = 'text';
  exampleInput.name = `claims[${i}].exampleValue`;
  exampleInput.value = claim.exampleValue || '';
  exampleInput.placeholder = 'Eksempelverdi';
  exampleInput.setAttribute('aria-label', `Eksempelverdi for ${claim.displayName || claim.path}`);
  exampleInput.addEventListener('input', () => {
    state.claims[i].exampleValue = exampleInput.value;
    schedulePreviewUpdate();
  });

  let exampleInputContainer = exampleInput;

  // For binary claims (images), add an upload button
  if (claim.type === 'binary') {
    const wrapper = document.createElement('div');
    wrapper.className = 'claim-preset-example-wrapper';

    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = 'image/*';
    fileInput.style.display = 'none';
    fileInput.addEventListener('change', (e) => {
      handleImageUpload(e, exampleInput, i);
    });

    const uploadBtn = document.createElement('button');
    uploadBtn.type = 'button';
    uploadBtn.className = 'ds-button';
    uploadBtn.setAttribute('data-variant', 'secondary');
    uploadBtn.setAttribute('aria-label', `Last opp bilde for ${claim.displayName || claim.path}`);
    uploadBtn.textContent = 'Last opp bilde';
    uploadBtn.addEventListener('click', (e) => {
      e.preventDefault();
      fileInput.click();
    });

    wrapper.appendChild(uploadBtn);
    wrapper.appendChild(fileInput);
    wrapper.appendChild(exampleInput);  // Hidden input for form submission
    exampleInputContainer = wrapper;
  }

  const removeBtn = document.createElement('button');
  removeBtn.type = 'button';
  removeBtn.className = 'ds-button claim-remove-btn';
  removeBtn.setAttribute('data-color', 'danger');
  removeBtn.setAttribute('data-variant', 'tertiary');
  removeBtn.setAttribute('aria-label', `Fjern ${claim.displayName || claim.path}`);
  removeBtn.textContent = '✕';
  removeBtn.addEventListener('click', () => {
    state.claims.splice(i, 1);

    // Check if removing this claim means the preset is no longer complete
    if (claim.presetKey) {
      const preset = PRESET_CLAIMS[claim.presetKey];
      const allPresent = preset.claims.every(p => state.claims.some(c => c.path === p.path));
      if (!allPresent) {
        state.activePresets.delete(claim.presetKey);
        syncPresetButtons();
      }
    }

    renderClaims();
    renderPreview();
    syncJsonTextarea();
  });

  div.appendChild(pathHidden);
  div.appendChild(nameHidden);
  div.appendChild(typeHidden);
  div.appendChild(mimeHidden);
  div.appendChild(label);
  div.appendChild(exampleInputContainer);
  div.appendChild(removeBtn);
  return div;
}

// ---------------------------------------------------------------------------
// Handle image upload for binary claims
// ---------------------------------------------------------------------------
function handleImageUpload(event, inputElement, claimIndex) {
  const file = event.target.files?.[0];
  if (!file) return;

  // Validate file is an image
  if (!file.type.startsWith('image/')) {
    alert('Kun bildefiler er tillatt');
    return;
  }

  // Limit file size to 1MB
  const MAX_SIZE = 1024 * 1024;
  if (file.size > MAX_SIZE) {
    alert('Bildefilen må være mindre enn 1MB');
    return;
  }

  const reader = new FileReader();
  reader.onload = (e) => {
    const dataUrl = e.target?.result;
    if (typeof dataUrl === 'string') {
      // Extract base64 part after 'data:image/...;base64,'
      const base64String = dataUrl.split(',')[1];
      if (base64String) {
        inputElement.value = base64String;
        state.claims[claimIndex].exampleValue = base64String;
        // Store the actual MIME type from the file
        state.claims[claimIndex].mimeType = file.type;
        schedulePreviewUpdate();
      } else {
        alert('Feil: kunne ikke konvertere bilde til base64');
      }
    }
  };
  reader.onerror = () => {
    alert('Feil ved lesing av bildefil');
  };
  reader.readAsDataURL(file);
}

// ---------------------------------------------------------------------------
// Preview panel (debounced, with spinner while pending)
// ---------------------------------------------------------------------------
let previewTimer = null;
const PREVIEW_DEBOUNCE_MS = 300;

function schedulePreviewUpdate() {
  const spinner = document.getElementById('preview-spinner');
  if (spinner) spinner.hidden = false;
  clearTimeout(previewTimer);
  previewTimer = setTimeout(() => {
    renderPreview();
    // Only sync JSON textarea if we are in schema mode (schema drives JSON)
    if (state.mode === 'schema') syncJsonTextarea();
    if (spinner) spinner.hidden = true;
  }, PREVIEW_DEBOUNCE_MS);
}

function renderPreview() {
  const panel = document.getElementById('preview-panel');
  if (!panel) return;

  // Apply colors to preview card (panel is the card)
  panel.style.backgroundColor = state.backgroundColor;
  panel.style.color = state.textColor;

  const nameEl = panel.querySelector('[data-preview-name]');
  if (nameEl) nameEl.textContent = state.name || 'Bevisnavn';

  const list = panel.querySelector('[data-preview-list]');
  if (!list) return;
  list.innerHTML = '';

  // Handle portrait separately
  const portraitClaim = state.claims.find(c => c.path === 'portrait' && c.type === 'binary');
  const portraitContainer = document.getElementById('preview-portrait');
  const portraitImg = document.getElementById('preview-portrait-img');
  if (portraitContainer && portraitImg) {
    if (portraitClaim && portraitClaim.exampleValue) {
      const mimeType = portraitClaim.mimeType || 'image/png';
      portraitImg.src = `data:${mimeType};base64,${portraitClaim.exampleValue}`;
      portraitContainer.hidden = false;
    } else {
      portraitContainer.hidden = true;
    }
  }

  state.claims.forEach(claim => {
    if (!claim.path) return;
    // Portrait is shown as image, not as a field row
    if (claim.path === 'portrait' && claim.type === 'binary') return;

    const row = document.createElement('div');
    row.className = 'preview-field';

    const label = document.createElement('span');
    label.className = 'preview-label';
    label.textContent = claim.displayName || claim.path;

    const val = document.createElement('span');
    val.className = 'preview-value';
    val.spellcheck = false;
    val.textContent = claim.type === 'binary' ? '[binærdata]' : (claim.exampleValue || '—');

    row.appendChild(label);
    row.appendChild(val);
    list.appendChild(row);
  });
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

  // Sync color inputs from state
  const bgColorInput = document.getElementById('bg-color-input');
  if (bgColorInput) bgColorInput.value = state.backgroundColor;

  const textColorInput = document.getElementById('text-color-input');
  if (textColorInput) textColorInput.value = state.textColor;

  renderClaims();
  renderPreview();
}

// ---------------------------------------------------------------------------
// Boot
// ---------------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', init);