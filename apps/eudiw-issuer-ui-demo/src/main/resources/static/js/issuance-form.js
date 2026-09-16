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
    'Dette beviset hentar data automatisk frå registeret ut frå personidentifikatoren. Ingen attributt kan redigerast.';

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

  function imageMimeFromBase64(value) {
    if (typeof value !== 'string') return null;
    if (value.indexOf('/9j/') === 0) return 'image/jpeg';
    if (value.indexOf('iVBOR') === 0) return 'image/png';
    return null;
  }

  function isImageValue(path, value) {
    return typeof value === 'string' &&
      (path.split('.').pop() === 'portrait' || imageMimeFromBase64(value) !== null);
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
      label: path.split('.').pop(),
      kind: kind,
      value: value,
      parentPath: parentPath || null,
      mime: kind === 'image' ? imageMimeFromBase64(value) : undefined
    };
  }

  function listFields(credentialData) {
    if (credentialData === null ||
        typeof credentialData !== 'object' ||
        Array.isArray(credentialData)) {
      return [];
    }

    // Deterministisk rekkjefølgje uavhengig av credential_data: direkte
    // felt alfabetisk først, nestede grupper sist.
    const directFields = [];
    const nestedFields = [];
    Object.keys(credentialData).forEach(function (key) {
      const value = credentialData[key];
      const direct = descriptor(key, value, null);

      if (direct) {
        directFields.push(direct);
        return;
      }

      if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
        const keys = Object.keys(value);
        // Only flatten an object when all of its values are primitive.
        if (keys.every(function (nestedKey) {
          return isPrimitive(value[nestedKey]);
        })) {
          keys.forEach(function (nestedKey) {
            const nested = descriptor(key + '.' + nestedKey, value[nestedKey], key);
            if (nested) nestedFields.push(nested);
          });
        }
        // Anything deeper or mixed is edited only in Avansert.
      }
    });
    const byPath = (a, b) => (a.path < b.path ? -1 : 1);
    directFields.sort(byPath);
    nestedFields.sort(byPath);
    return directFields.concat(nestedFields);
  }

  function hasOwn(object, key) {
    return Object.prototype.hasOwnProperty.call(object, key);
  }

  function credentialDataFor(object) {
    if (object === null || typeof object !== 'object' || Array.isArray(object)) {
      throw new Error('IssuanceDefinition må vere eit objekt.');
    }
    if (!hasOwn(object, 'credential_data') ||
        object.credential_data === null ||
        typeof object.credential_data !== 'object' ||
        Array.isArray(object.credential_data)) {
      throw new Error('credential_data må vere eit objekt.');
    }
    return object.credential_data;
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
    if (typeof rawValue !== 'boolean') {
      throw new Error('Feltet må vere sant eller usant.');
    }
    return rawValue;
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
      throw new Error('Feltet kan ikkje redigerast i skjemamodus.');
    }

    target.parent[target.leaf] = value;
    return object;
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
      if (!alert) return;
      alert.hidden = true;
      // Tøm teksten, så same feilmelding blir lesen opp på nytt neste gong.
      alert.textContent = '';
    }

    function showAlert(message, panelId) {
      let alert = document.getElementById('issuance-json-error');
      if (!alert) {
        alert = document.createElement('div');
        alert.id = 'issuance-json-error';
        alert.className = 'ds-alert';
        alert.setAttribute('data-color', 'danger');
        // role="alert" gir aria-live="assertive" og aria-atomic="true".
        alert.setAttribute('role', 'alert');
      }
      const panel = document.getElementById(panelId || 'json-panel');
      if (panel && alert.parentElement !== panel) panel.prepend(alert);
      // Vis elementet før teksten blir sett, så live-regionen ligg i
      // tilgjengelegheitstreet når innhaldet endrar seg.
      alert.hidden = false;
      alert.textContent = message;
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
      const groupContainer = document.getElementById('issuance-groups');
      const empty = document.getElementById('issuance-empty');
      if (!container) return;
      while (container.firstChild) container.removeChild(container.firstChild);
      if (groupContainer) {
        while (groupContainer.firstChild) groupContainer.removeChild(groupContainer.firstChild);
      }

      const data = currentObject && currentObject.credential_data;
      const fields = listFields(data);
      if (empty) {
        empty.textContent = EMPTY_COPY;
        empty.hidden = fields.length !== 0;
      }
      const resetButton = document.getElementById('issuance-reset');
      if (resetButton) resetButton.hidden = fields.length === 0;
      if (!fields.length) return;

      let lastGroup = null;
      let fieldsContainer = container;
      fields.forEach(function (field) {
        if (field.parentPath !== lastGroup) {
          if (lastGroup) {
            fieldsContainer = container;
          }
          lastGroup = field.parentPath;
          if (lastGroup) {
            const group = document.createElement('div');
            group.className = 'claim-group';
            const heading = document.createElement('h3');
            heading.className = 'ds-heading claim-group-heading';
            heading.setAttribute('data-size', 'sm');
            heading.textContent = lastGroup;
            group.appendChild(heading);
            const groupFields = document.createElement('div');
            groupFields.className = 'claim-group__fields';
            group.appendChild(groupFields);
            groupContainer.appendChild(group);
            fieldsContainer = groupFields;
          }
        }
        const claim = document.createElement('div');
        claim.className = 'claim';
        const label = document.createElement('label');
        label.className = 'claim-preset-label';
        label.textContent = field.label;
        claim.appendChild(label);
        const input = document.createElement('input');
        label.htmlFor = input.id =
          'issuance-' + field.path.replace(/[^a-zA-Z0-9_-]/g, '-');

        // Bilet-feltet legg input inne i knappe-wrapperen, dei andre rett i .claim
        let inputParent = claim;

        if (field.kind === 'image') {
          const image = document.createElement('img');
          image.src = imageSource(field);
          image.alt = field.label;
          const frame = document.createElement('div');
          frame.className = 'preview-card__portrait';
          frame.appendChild(image);
          const wrapper = document.createElement('div');
          wrapper.className = 'claim-preset-example-wrapper';
          // Bilete og opplastingsknapp i éin boks, så .claim held seg til to
          // rutenettceller (etikett + verdi) som dei andre felta.
          wrapper.appendChild(frame);
          input.type = 'file';
          input.accept = 'image/*';
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
          claim.appendChild(wrapper);
          inputParent = wrapper;
        } else if (field.kind === 'boolean') {
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

        inputParent.appendChild(input);
        fieldsContainer.appendChild(claim);
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
        showAlert(error.message, 'schema-panel');
      }
    }

    function switchMode(nextMode, event) {
      if (nextMode !== 'schema' && nextMode !== 'json') return;
      if (nextMode === 'schema') {
        if (!validateTextarea()) {
          event?.preventDefault();
          showAlert('Ugyldig JSON. Rett feilen i Avansert før du går tilbake til Skjema.');
          return;
        }
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
      setBadge(originalObject !== null);
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
      if (!validateTextarea()) {
        event.preventDefault();
        const jsonTab = document.getElementById('json-tab');
        if (jsonTab) jsonTab.click();
        showAlert('Ugyldig JSON. Rett feilen før du sender inn skjemaet.');
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
    init: initDom
  };
}));
