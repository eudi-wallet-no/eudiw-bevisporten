# eudiw-bevisporten

> [!NOTE]
> Del av National Sandbox for Digital Wallet.
> Se https://docs.digdir.no/docs/lommebok/lommebok_om.html for mer informasjon.

Monorepo for EUDI Wallet Verifiable Credential Issuance (VCI) og Verifiable Presentation (VP) applikasjoner.

## Applikasjoner

| Applikasjon | Beskrivelse | README |
| --- | --- | --- |
| [eudiw-authoritativ-sources-connector](apps/eudiw-authoritativ-sources-connector) | Authoritative Sources Connector | [README](apps/eudiw-authoritativ-sources-connector/README.md) |
| eudiw-byob-service | Bring Your Own Briefcase Service | |
| eudiw-oauth-server | OAuth Server | |
| eudiw-issuer-ui | Issuer UI (Bevisporten) | |
| eudiw-verifier-demo | Verifier Demo | |
| eudiw-issuer-server | Issuer Server | |
| eudiw-status-list | Status List Service | |

## Struktur

```
apps/       Applikasjoner (én mappe per app)
docs/       Delt dokumentasjon på tvers av applikasjoner
```

Hver app har sin egen README med detaljer om oppsett, kjøring og testing.

## Lokal utvikling

Se individuell app sin README for spesifikke instruksjoner.
