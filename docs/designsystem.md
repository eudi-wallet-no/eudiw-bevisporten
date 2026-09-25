# Designsystemet

Dette dokumentet er den autoritative retningslinjen for UI-endringer i dette repoet.

Bruk dette som hovedkilde for komponentvalg, tokens, semantikk og tilgjengelighet. Ikke bygg ny markup eller egen CSS uten å først sjekke dette dokumentet og den relevante Designsystemet-dokumentasjonen for valgt komponent.

## Hva vi må følge

- Bruk Designsystemet-komponenter og tokens i stedet for egne løsninger.
- Hold HTML semantisk og tilgjengelig: `button` for handlinger, `a` for navigasjon, `label`/`fieldset`/`legend` for skjema, riktig heading-hierarki.
- Bruk Designsystemet-tokens for spacing, farger, typografi og states: `var(--ds-*)` og `var(--dsc-*)`.
- Unngå hardkodet CSS, egen variant av komponenter eller nye temaer uten tydelig behov.
- Følg WCAG 2.1 AA som minimum; vi sikter mot WCAG 2.2 AA og AAA der det er praktisk mulig.

## Baseline for repoet

- For nye/endrede komponenter: bruk eksisterende `ds-*`-klasser før ny CSS.
- Gjenbruk eksisterende mønstre og struktur i appen før du lager egne varianter.
- Korriger markupsfeil før du finjusterer styling.
- Hvis en komponent trenger ekstra styling, gjør det med små, målrettede CSS-overstyringer i appens eget stylesheet, ikke med nye DS-løsninger.

## Designsystemet: komponent- og pattern-regler

### Markup

- Semantikk først: `button`, `a`, `label`, `fieldset`, `legend`, `main`, `header`, `nav`, `footer`.
- Bruk `data-color`, `data-size`, `data-variant` kun når komponentdokumentasjonen har dem.
- Bruk `aria-current="page"` for aktiv navigasjon.
- Legg til `aria-label` dersom ikoner er eneste innhold i interaktive elementer.
- Sørg for at fokusindikator alltid er synlig for tastaturbruk.

### Tokens og stil

- Bruk semantiske fargereferanser: `var(--ds-color-...)`, `var(--ds-size-...)`, `var(--ds-border-radius-...)`, `var(--ds-font-weight-...)`.
- Unngå hex-koder, hardkodede `px`-verdier og “magiske” spacing-verdier.
- Behold eksisterende spacing- og layoutmønstre i appen; endre bare det som faktisk er nødvendig.

### Formulardeler

- Alle felter skal ha synlig label eller alternativ godt definert `aria-label`. Placeholder er ikke nok.
- Feilmeldinger skal brukes med `data-field="validation"` og `role="alert"` der dette er relevant.
- Når et felt har feil, sett `aria-invalid="true"` og koble feilen via `aria-describedby`.

### Navigasjon og layout

- Skip-link til hovedinnhold skal være i toppen av siden.
- Navigasjonslenker skal ha tydelig aktiv status og være tilgjengelige med tastatur.
- Behold logisk heading-hierarki: `h1` → `h2` → `h3` uten hopp.

## Tilgjengelighet

Følg WCAG 2.1 AA som minimum. Vi bør oppnå WCAG 2.2 AA og AAA der det er praktisk mulig.

Krav som er spesielt viktige i dette repoet:

- 4.5:1 kontrast for tekst og fokusmarkering.
- Synlig fokus på alle interaktive elementer.
- Ingen informasjon kun gitt via farge.
- 44×44 px touchmål på interaktive elementer.
- `lang` på html-dokumentet.
- Skjermlesbar semantikk og programmatisk navn/rolle/verdi for UI-komponenter.
- Dynamisk oppdatert innhold skal annonseres med `aria-live`/`role="status"` eller `role="alert"` når det er riktig.

## Kilder og oppdatering

Bruk de offisielle Designsystemet-dokumentene for komponentspesifikk info:

- `https://designsystemet.no/en/fundamentals`
- `https://designsystemet.no/en/components/docs/<component>/code`
- `https://designsystemet.no/en/components/docs/<component>/accessibility`

Når vi trenger en versjonsnøytral eller komponentspesifikk implementasjon, skal vi bruke de oppdaterte dokumentene som autoritet og ikke gjette fra minne eller ustrukturert eksperttenkning.

## Sjekkliste før merge

- Har du brukt eksisterende DS-komponenter før ny markup?
- Har du brukt riktige semantiske elementer og tilgjengelighetsattributter?
- Har du brukt tokens i stedet for egne farger/spacings?
- Er kontrast, fokus og labels i orden?
- Er implementasjonen konsistent med andre sider i appen?
- Er det dokumentert dersom vi avviker fra standardmønsteret?
