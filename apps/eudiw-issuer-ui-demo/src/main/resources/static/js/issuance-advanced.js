const personIdentifier = document.getElementById('person_identifier');
const issuanceMode = document.getElementById('issuance_mode');

document.addEventListener('modechange', function (event) {
  queueMicrotask(function () {
    if (!personIdentifier || !issuanceMode || event.defaultPrevented) {
      return;
    }

    const isAdvanced = event.detail.mode === 'json';
    personIdentifier.ariaDisabled = isAdvanced;
    issuanceMode.value = event.detail.mode;
  });
});