---
name: norsk-klarsprak
description: "Edit Norwegian UI copy, documentation, and product text for clarity. Preserve the existing Bokmål or Nynorsk form, and ask the user when the text mixes both."
license: Digitaliseringsdirektoratet
allowed-tools: ['view', 'grep', 'glob', 'ask_user']
---

# Norsk klarspråk

Skriv norsk tekst slik at målgruppa finn informasjonen, forstår han og kan bruke han. Gjeld UI-tekst, mikrotekst, dokumentasjon, README, produkttekst, feilmeldingar, hjelpetekst, stadfestingar og språkvask.

## Vel målform først

Les teksten rundt endringa før du redigerer.

1. Er teksten tydeleg på bokmål eller nynorsk, held du fram på same målform.
2. Har filene kvar si målform, bevarer du målforma i kvar fil.
3. Er målformene blanda i same tekst, eller skal du skrive ny tekst utan språkleg kontekst, spør du brukaren.

Fagtermar, sitat, kode, namn og tekst på andre språk er ikkje ei blanding. Sjå etter fleire tydelege språklege trekk, ikkje eitt enkeltord.

## Skriveregler

- Éin idé per setning, eitt tema per avsnitt. Verbet tidleg, aktiv form. Inga fast ordgrense.
- Del setningar med innskot og fleire komma når kommaet skil sjølvstendige poeng. Bevar komma rettskrivinga krev.
- Start med det lesaren treng å vite eller gjere, og plasser det viktigaste først. Flytt, grupper eller del opp innhaldet når det trengst.
- Bruk verb, ikkje substantiv av verb: «Vi vurderer løysinga», ikkje «Vi gjennomfører ei vurdering av løysinga».
- Skriv kva som skjer, ikkje at noko «mogleggjer», «legg til rette for» eller «effektiviserer». Skriv kva som løyser problemet, ikkje at det blir «adressert».
- Kutt opningar som «det er viktig å merke seg» og «i lys av dette». Ikkje gjenta poenget i ei oppsummering.
- Bruk vanlege, konkrete ord. Skriv «du» og «vi» når teksten vender seg til folk.
- Kutt fylltekst, sjølvskryt, superlativ og moteord. Skriv sakleg og kollegialt, ikkje som ei pressemelding, og følg lokale stilreglar for teiknsetjing og overskrifter.
- Bruk norske ord for unødvendige anglisismar: «bruke» for «utnytte», «innspel» for «input», «ha ansvar for» for «ta eigarskap til». Bruk bindestrek i samansetningar med engelske fagord når rettskrivinga krev det.
- Bruk omgrepslista til prosjektet, elles [Termportalen](https://www.termportalen.no/) med rett domene og målform. Same omgrep for same ting overalt. Ikkje byt ut ein presis fagterm fordi han er vanskeleg — forklar han når målgruppa treng det.
- Les teksten høgt til slutt og kutt det som er passivt, uklart eller overforklarande.

## UI-tekst

- **Knappar:** handlinga eller resultatet, presist og konsekvent. Kortast mogleg utan å bli tvitydig.
- **Lenkjer:** kva lenkja fører til. Ikkje «Klikk her» eller «Les meir» åleine.
- **Feilmeldingar:** kva som gjekk gale, og kva personen kan gjere.
- **Hjelpetekst:** feltet eller valet, ikkje interne systemdetaljar.
- **Stadfestingar:** konkret kva som er lagra, sendt eller endra.
- **Kort og steg:** handlinga først. Resultatet berre når det hjelper personen å velje.
- Bruk same brukaromgrep i feltetikettar og valideringsmeldingar. Vis tekniske felt-ID-ar berre når dei hjelper, og skil dei tydeleg frå etiketten.
- Når fleire valideringsfeil blir samla i ei feilliste, skal kvar melding vere éi linje. Ikkje legg inn linjeskift i sjølve meldinga.
- Skriv eksplisitt kva som må følgje ein spesifikasjon eller regel. Unngå pronomen med uklar referanse.

Lenkje- og knappetekst skal gi meining åleine. Vurder teksten saman med komponenten, plasseringa og utforminga. Kontroller heile løpet — navigasjon, ledetekstar, knappar, hjelpetekstar, feilmeldingar og kvitteringar — og bruk same omgrep på tvers av skjermbilete og kanalar.

## Inkluderande språk

Skriv direkte til dei som bruker tenesta. Unngå unødvendige føresetnader om kjønn, familie, arbeidsevne, diagnose, språk eller behov, og bruk kjønnsnøytrale formuleringar. Sjå klarspråk, universell utforming og tenestedesign i samanheng, og involver personane det gjeld når behova varierer.

## Grenser

**Alltid:** bevar eller styrk klarspråket, bevar målforma, la lenkje- og knappetekst gi meining åleine, og kontroller at teksten framleis er fagleg korrekt.

**Spør først:** når målforma er blanda eller manglar, når endringa kan påverke fagleg innhald, eller når heile sider skal omstrukturerast eller seksjonar fjernast.

**Aldri:** endre fakta eller faglege avgjerder som del av språkvask, blande bokmål og nynorsk i same tekst, leggje til innhald kjelda ikkje støttar, eller bruke KI-prega fylltekst.

Du kan ikkje slå fast at ein tekst er klar ut frå eiga vurdering. Tilrå brukartesting med folk i målgruppa når teksten er vesentleg, risikofylt eller del av ei sentral tenestereise.

## Kjelder

- [Språkrådet om klarspråk](https://sprakradet.no/klarsprak/)
- [Språk i digitale tenester](https://sprakradet.no/klarsprak/om-skriving/sprak-i-digitale-tenester/)
- [Brukartesting av tekstar](https://sprakradet.no/klarsprak/brukartesting-av-tekstar/)
- [Digdirs rettleiar om klarspråk i digitale tenester](https://www.digdir.no/klart-sprak/rettleiar-om-klarsprak-i-utvikling-av-digitale-tenester/3564)
- [KS-rettleiaren for inkluderande språk](https://www.ks.no/inkluderende-sprak)
