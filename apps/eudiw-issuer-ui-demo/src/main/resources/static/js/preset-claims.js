/**
 * preset-claims.js
 *
 * Forhåndsdefinerte feltgrupper («hurtigkonfigurasjon») for beviseditor.
 * Legg til en ny gruppe her — knappene i skjemamodus genereres automatisk.
 *
 * Hvert claim: { path, displayName, type, mimeType, exampleValue }
 * - path: claim-sti i beviset
 * - displayName: visningsnavn i skjema og preview
 * - type: 'string' | 'binary' (binary vises med bildeopplasting)
 * - mimeType: MIME-type for binary-claims, ellers null
 * - exampleValue: eksempelverdi som vises i skjema og preview
 */
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
        // 96×96 gray person-silhouette placeholder PNG
        exampleValue: 'iVBORw0KGgoAAAANSUhEUgAAAGAAAABgCAIAAABt+uBvAAABDElEQVR42u3ayRHDIBBFQeUfzQ9Jofjug8qSxTLQL4KpLi7AHKcuOxAAAgQIECBAgAAJEKANgJIA+ha5bl+g3GkvoDxtC6D81+JAeaNlgfJeCwLl7ZYCSpsWAUrLAK0OlPYBArQtUHoFCBAgQIAAFdVpauQEAQIECBAgQG7zgAB5kwZUFOj0LzbEqM/Y/YBOf/PdjHoO3BvotB/U1Kj/qGOAHjCNGnIk0O9GAycEBAgQIECAAAEC5KrhNu/BrC6QJ9feNNaAbZhtvmGWEZUByrgKAGV0gCoDZY4mBcpMAaoGlPmaCCizBgjQAkCZO0BzA6VCgAAVBUqdAAECtBpQqgUIECBAgAABAgToTh/IbgDJoD1MpAAAAABJRU5ErkJggg=='
      }
    ]
  }
};
