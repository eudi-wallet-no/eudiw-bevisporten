const assert = require('assert');
const {
  parseIssuanceJson,
  serializeIssuance,
  listFields,
  applyField
} = require('../../main/resources/static/js/issuance-form.js');

function payload(credentialData) {
  const value = {
    credential_issuer: 'https://issuer.example',
    credential_configuration_id: 'pid',
    subject: { identifier: '05821098825' }
  };
  if (arguments.length > 0) value.credential_data = credentialData;
  return value;
}

const pidJson = JSON.stringify(payload({
  given_name: 'Ola',
  place_of_birth: { country: 'NO' },
  nationality: ['NO']
}));
assert.deepStrictEqual(
  JSON.parse(serializeIssuance(parseIssuanceJson(pidJson))),
  JSON.parse(pidJson)
);
const pidFields = listFields(JSON.parse(pidJson).credential_data);
assert(pidFields.some(field =>
  field.path === 'place_of_birth.country' &&
  field.kind === 'string' &&
  field.value === 'NO'
));
assert(pidFields.some(field =>
  field.path === 'nationality' &&
  field.kind === 'string-list' &&
  Array.isArray(field.value)
));
assert(!pidFields.some(field => field.path === 'place_of_birth'));

const countryPayload = parseIssuanceJson(pidJson);
applyField(countryPayload, 'place_of_birth.country', 'SE');
assert.deepStrictEqual(countryPayload.credential_data.place_of_birth, { country: 'SE' });
applyField(countryPayload, 'nationality', 'NO, SE');
assert.deepStrictEqual(countryPayload.credential_data.nationality, ['NO', 'SE']);

const mdl = payload({
  age_over_18: true,
  age_in_years: 40,
  document_number: 123456789
});
const mdlFields = listFields(mdl.credential_data);
assert.strictEqual(mdlFields.find(field => field.path === 'age_over_18').kind, 'boolean');
assert.strictEqual(mdlFields.find(field => field.path === 'age_in_years').kind, 'integer');
applyField(mdl, 'age_over_18', true);
applyField(mdl, 'age_in_years', '40');
assert.strictEqual(mdl.credential_data.age_over_18, true);
assert.strictEqual(mdl.credential_data.age_in_years, 40);
assert.strictEqual(mdl.credential_data.document_number, 123456789);
assert.throws(() => applyField(mdl, 'age_in_years', '40.5'));
assert.strictEqual(mdl.credential_data.age_in_years, 40);

const withoutCredentialData = payload();
assert.deepStrictEqual(listFields(withoutCredentialData.credential_data), []);
assert(!Object.prototype.hasOwnProperty.call(
  parseIssuanceJson(serializeIssuance(withoutCredentialData)),
  'credential_data'
));

const portrait = payload({ portrait: '/9j/' + 'A'.repeat(40) });
const portraitField = listFields(portrait.credential_data)[0];
assert.strictEqual(portraitField.kind, 'image');
assert.strictEqual(portraitField.mime, 'image/jpeg');

const pid = payload({ personal_identifier: '05821098825' });
applyField(pid, 'personal_identifier', '05821098825');
assert.strictEqual(typeof pid.credential_data.personal_identifier, 'string');
assert.strictEqual(pid.credential_data.personal_identifier, '05821098825');

console.log('issuance-form.check.js: all assertions passed');
