/**
 * Render and edit the credential_data part of an IssuanceDefinition.
 *
 * The field ceiling is deliberately one level. Upgrade to recursive field
 * rendering when deeper credential data needs to be edited.
 */
(function (root, factory) {
  const api = factory();

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
  }
  if (root) {
    root.IssuanceForm = api;
  }

  // Do not touch document while this file is imported by Node.
  if (typeof document !== 'undefined' &&
      document.getElementById('send_attributes_form')) {
    api.init();
  }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  const EMPTY_COPY =
    'Dette beviset hentar data automatisk frå registeret ut frå personidentifikator, og har inga redigerbare attributtar.';

  function parseIssuanceJson(str) {
    const parsed = JSON.parse(str);
    if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('JSON-et må vere eit objekt.');
    }
    return parsed;
  }

  function serializeIssuance(obj) {
    return JSON.stringify(obj, null, 2);
  }

  function isPrimitive(value) {
    return typeof value === 'string' ||
      typeof value === 'boolean' ||
      (typeof value === 'number' && Number.isFinite(value));
  }

  function sniffImageMime(value) {
    if (typeof value !== 'string') return null;
    if (value.indexOf('/9j/') === 0) return 'image/jpeg';
    if (value.indexOf('iVBOR') === 0) return 'image/png';
    return null;
  }

  function isImageValue(path, value) {
    return typeof value === 'string' &&
      (path.split('.').pop() === 'portrait' || sniffImageMime(value) !== null);
  }

  function kindForValue(path, value) {
    if (isImageValue(path, value)) return 'image';
    if (Array.isArray(value) && value.every(isPrimitive)) return 'string-list';
    if (typeof value === 'string') return 'string';
    if (typeof value === 'boolean') return 'boolean';
    if (typeof value === 'number' && Number.isInteger(value)) return 'integer';
    return null;
  }

  function descriptor(path, value, parentPath) {
    const kind = kindForValue(path, value);
    if (!kind) return null;
    return {
      path: path,
      label: path,
      kind: kind,
      value: value,
      parentPath: parentPath || null,
      mime: kind === 'image' ? sniffImageMime(value) : undefined
    };
  }

  function listFields(credentialData) {
    if (credentialData === null ||
        typeof credentialData !== 'object' ||
        Array.isArray(credentialData)) {
      return [];
    }

    const fields = [];
    Object.keys(credentialData).forEach(function (key) {
      const value = credentialData[key];
      const direct = descriptor(key, value, null);

      if (direct) {
        fields.push(direct);
        return;
      }

      if (Array.isArray(value)) {
        if (value.every(isPrimitive)) {
          fields.push({
            path: key,
            label: key,
            kind: 'string-list',
            value: value,
            parentPath: null
          });
        }
        return;
      }

      if (value !== null && typeof value === 'object') {
        const keys = Object.keys(value);
        // Only flatten an object when all of its values are primitive.
        if (keys.every(function (nestedKey) {
          return isPrimitive(value[nestedKey]);
        })) {
          keys.forEach(function (nestedKey) {
            const path = key + '.' + nestedKey;
            const nested = descriptor(path, value[nestedKey], key);
            if (nested) fields.push(nested);
          });
        }
        // Anything deeper or mixed is edited only in Avansert.
      }
    });
    return fields;
  }

  function hasOwn(object, key) {
    return Object.prototype.hasOwnProperty.call(object, key);
  }

  function credentialDataFor(object) {
    if (object === null || typeof object !== 'object' || Array.isArray(object)) {
      throw new Error('IssuanceDefinition må vere eit objekt.');
    }
    if (hasOwn(object, 'credential_data')) {
      if (object.credential_data === null ||
          typeof object.credential_data !== 'object' ||
          Array.isArray(object.credential_data)) {
        throw new Error('credential_data må vere eit objekt.');
      }
      return object.credential_data;
    }
    // This also makes the pure function convenient when passed credential_data.
    return object;
  }

  function valueAtPath(data, path) {
    const parts = path.split('.');
    let parent = data;
    for (let index = 0; index < parts.length - 1; index += 1) {
      const part = parts[index];
      if (!hasOwn(parent, part) ||
          parent[part] === null ||
          typeof parent[part] !== 'object' ||
          Array.isArray(parent[part])) {
        throw new Error('Ukjend feltsti: ' + path);
      }
      parent = parent[part];
    }
    const leaf = parts[parts.length - 1];
    if (!hasOwn(parent, leaf)) {
      throw new Error('Ukjend feltsti: ' + path);
    }
    return { parent: parent, leaf: leaf, value: parent[leaf] };
  }

  function coerceBoolean(rawValue) {
    if (typeof rawValue === 'boolean') return rawValue;
    if (rawValue === 'true') return true;
    if (rawValue === 'false') return false;
    throw new Error('Feltet må vere sant eller usant.');
  }

  function coerceInteger(rawValue) {
    if (rawValue === '' || rawValue === null || rawValue === undefined) {
      throw new Error('Talet kan ikkje vere tomt.');
    }
    if (typeof rawValue === 'string' && !/^[+-]?\d+$/.test(rawValue.trim())) {
      throw new Error('Talet må vere eit heiltal.');
    }
    const number = typeof rawValue === 'number' ? rawValue : Number(rawValue);
    if (!Number.isSafeInteger(number)) {
      throw new Error('Talet må vere eit trygt heiltal.');
    }
    return number;
  }

  function applyField(object, path, rawValue) {
    const data = credentialDataFor(object);
    const target = valueAtPath(data, path);
    const kind = kindForValue(path, target.value);
    let value;

    if (kind === 'boolean') {
      value = coerceBoolean(rawValue);
    } else if (kind === 'integer') {
      value = coerceInteger(rawValue);
    } else if (kind === 'string-list') {
      const text = Array.isArray(rawValue) ? rawValue.join(',') : String(rawValue);
      value = text.trim() === '' ? [] : text.split(',').map(function (item) {
        return item.trim();
      }).filter(function (item) {
        return item !== '';
      });
    } else if (kind === 'string' || kind === 'image') {
      value = String(rawValue);
    } else {
      throw new Error('Feltet kan ikkje redigerast i Skjema.');
    }

    target.parent[target.leaf] = value;
    return object;
  }

  function roundTripUnedited(str) {
    return serializeIssuance(parseIssuanceJson(str));
  }

  function initDom() {
    const form = document.getElementById('send_attributes_form');
    const textarea = document.getElementById('eaa_data_field');
    if (!form || !textarea) return;

    let currentObject = null;
    let originalObject = null;
    const originalText = textarea.value;
    const MAX_SIZE = 1024 * 1024;

    function setBadge(valid) {
      const badge = document.getElementById('json-badge');
      if (!badge) return;
      badge.textContent = valid ? '✓ Gyldig JSON' : '✗ Ugyldig JSON';
      badge.className = valid
        ? 'json-badge json-badge--valid'
        : 'json-badge json-badge--invalid';
    }

    function hideAlert() {
      const alert = document.getElementById('issuance-json-error');
      if (alert) alert.hidden = true;
    }

    function showAlert(message, panelId) {
      let alert = document.getElementById('issuance-json-error');
      if (!alert) {
        alert = document.createElement('div');
        alert.id = 'issuance-json-error';
        alert.className = 'ds-alert';
        alert.setAttribute('data-color', 'danger');
      }
      const panel = document.getElementById(panelId || 'json-panel');
      if (panel && alert.parentElement !== panel) panel.prepend(alert);
      alert.textContent = message;
      alert.hidden = false;
    }

    function validateTextarea() {
      try {
        const parsed = parseIssuanceJson(textarea.value);
        setBadge(true);
        return parsed;
      } catch (_) {
        setBadge(false);
        return null;
      }
    }

    function imageSource(field) {
      if (String(field.value).indexOf('data:') === 0) return field.value;
      return 'data:' + (field.mime || 'image/jpeg') + ';base64,' + field.value;
    }

    function renderFields() {
      const container = document.getElementById('issuance-fields');
      const empty = document.getElementById('issuance-empty');
      if (!container) return;
      while (container.firstChild) container.removeChild(container.firstChild);

      const data = currentObject && currentObject.credential_data;
      const fields = listFields(data);
      if (empty) {
        empty.textContent = EMPTY_COPY;
        empty.hidden = fields.length !== 0;
      }
      const resetButton = document.getElementById('issuance-reset');
      if (resetButton) resetButton.hidden = fields.length === 0;
      if (!fields.length) return;

      fields.forEach(function (field) {
        const claim = document.createElement('div');
        claim.className = 'claim';
        const label = document.createElement('label');
        label.className = 'claim-preset-label';
        label.textContent = field.label;
        claim.appendChild(label);

        let input;
        if (field.kind === 'image') {
          const image = document.createElement('img');
          image.src = imageSource(field);
          image.alt = field.label;
          const frame = document.createElement('div');
          frame.className = 'preview-card__portrait';
          frame.appendChild(image);
          claim.appendChild(frame);
          const wrapper = document.createElement('div');
          wrapper.className = 'claim-preset-example-wrapper';
          input = document.createElement('input');
          input.type = 'file';
          input.accept = field.mime || 'image/*';
          input.style.display = 'none';
          input.addEventListener('change', function () {
            const file = input.files && input.files[0];
            if (!file) return;
            if (file.size > MAX_SIZE) {
              showAlert('Biletet må vere mindre enn 1 MB.', 'schema-panel');
              return;
            }
            const reader = new FileReader();
            reader.onload = function () {
              const result = String(reader.result);
              const base64 = result.replace(/^data:[^;]+;base64,/, '');
              image.src = result;
              editField(field, base64);
            };
            reader.readAsDataURL(file);
          });
          const uploadBtn = document.createElement('button');
          uploadBtn.type = 'button';
          uploadBtn.className = 'ds-button';
          uploadBtn.setAttribute('data-variant', 'secondary');
          uploadBtn.setAttribute('aria-label', 'Last opp bilete for ' + field.label);
          uploadBtn.textContent = 'Last opp bilete';
          uploadBtn.addEventListener('click', function () {
            input.click();
          });
          wrapper.appendChild(uploadBtn);
          wrapper.appendChild(input);
          input.id = 'issuance-' + field.path.replace(/[^a-zA-Z0-9_-]/g, '-');
          claim.appendChild(wrapper);
          container.appendChild(claim);
          return;
        } else {
          input = document.createElement('input');
          if (field.kind === 'boolean') {
            input.className = 'ds-input';
            input.type = 'checkbox';
            input.checked = field.value;
            input.addEventListener('change', function () {
              editField(field, input.checked);
            });
          } else {
            input.className = 'ds-input claim-preset-example';
            input.type = field.kind === 'integer' ? 'number' : 'text';
            if (field.kind === 'integer') input.step = '1';
            input.value = field.kind === 'string-list'
              ? field.value.join(', ')
              : field.value;
            input.addEventListener('input', function () {
              editField(field, input.value);
            });
          }
        }

        label.htmlFor = input.id = 'issuance-' +
           field.path.replace(/[^a-zA-Z0-9_-]/g, '-');
        claim.appendChild(input);
        container.appendChild(claim);
      });
    }

    function editField(field, rawValue) {
      if (!currentObject) return;
      try {
        applyField(currentObject, field.path, rawValue);
        textarea.value = serializeIssuance(currentObject);
        setBadge(true);
        hideAlert();
      } catch (error) {
        showAlert(error.message);
      }
    }

    function switchMode(nextMode, event) {
      if (nextMode !== 'schema' && nextMode !== 'json') return;
      if (nextMode === 'schema') {
        const parsed = validateTextarea();
        if (!parsed) {
          event?.preventDefault();
          showAlert('Ugyldig JSON. Rett feilen i Avansert før du går tilbake til Skjema.');
          return;
        }
        currentObject = parsed;
        renderFields();
        hideAlert();
      }
      if (nextMode === 'json') validateTextarea();
    }

    function reset() {
      textarea.value = originalText;
      currentObject = originalObject === null
        ? null
        : parseIssuanceJson(serializeIssuance(originalObject));
      const parsed = validateTextarea();
      if (parsed) currentObject = parsed;
      renderFields();
      hideAlert();
    }

    try {
      currentObject = parseIssuanceJson(textarea.value);
      originalObject = parseIssuanceJson(serializeIssuance(currentObject));
      setBadge(true);
    } catch (_) {
      setBadge(false);
    }
    renderFields();

    textarea.addEventListener('input', function () {
      const parsed = validateTextarea();
      if (parsed) currentObject = parsed;
    });
    form.addEventListener('submit', function (event) {
      const parsed = validateTextarea();
      if (!parsed) {
        event.preventDefault();
        const jsonTab = document.getElementById('json-tab');
        if (jsonTab) jsonTab.click();
        showAlert('Ugyldig JSON. Rett feilen før du sender inn skjemaet.');
      } else {
        currentObject = parsed;
      }
    });
    document.addEventListener('modechange', function (event) {
      if (event.detail && event.detail.mode) switchMode(event.detail.mode, event);
    });
    const resetButton = document.getElementById('issuance-reset');
    if (resetButton) resetButton.addEventListener('click', reset);
  }

  return {
    parseIssuanceJson: parseIssuanceJson,
    serializeIssuance: serializeIssuance,
    listFields: listFields,
    applyField: applyField,
    roundTripUnedited: roundTripUnedited,
    init: initDom
  };
}));
