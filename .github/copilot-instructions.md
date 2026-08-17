# JIRA-ID i branch og PR-tittel

Alle oppgaver har en JIRA-ID. **Spør alltid brukeren om JIRA-ID** hvis den ikke er oppgitt —
ikke anta eller finn på en. Brukes i PR-tittel:

```
<JIRA-ID>: <PR-tittel>
```

F.eks. `EUW-1234: Ny pr`. Uten dette feiler `validate-pr-title`-sjekken i GitHub Actions.

PR-tittelen (delen etter JIRA-ID-prefikset) skal skrives på **norsk**.

## Branch-navn

Branch-navnet skal alltid være **brukernavn + JIRA-ID**, ikke en fritekstbeskrivelse. Plattformen
legger automatisk til brukernavnet som prefiks (se `rename_branch`-verktøyet), så bruk selve
JIRA-ID-en (små bokstaver) som navnet du sender inn:

```
rename_branch(name: "<jira-id>")   →  gir branch: <brukernavn>-<jira-id>
```

F.eks. `rename_branch(name: "euw-1234")` gir `andreasbalevik-euw-1234`. Ikke bruk beskrivende
kebab-case-navn (som `fix-login-validation`) med mindre oppgaven mangler en JIRA-ID.

# PR-beskrivelser

Skriv menneskelig, kort og konkret. Unngå AI-språk ("This PR introduces..."), emojis, overdreven struktur, selvskryt og oppsummeringsvegger. Én linje holder for trivielle endringer.

Mal:
```
## Hva og hvorfor
1-2 setninger: hva endres og hvorfor.

## Endringer
- Viktigste punkter, ikke hver fil/detalj.
```
