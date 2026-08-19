# eudiw-bevisporten

> [!NOTE]
> Del av National Sandbox for Digital Wallet.
> Se https://docs.digdir.no/docs/lommebok/lommebok_om.html for mer informasjon.

Monorepo for EUDI Wallet Verifiable Credential Issuance (VCI) og Verifiable Presentation (VP) applikasjoner.

## Applikasjoner

| Applikasjon | Beskrivelse | README |
| --- | --- | --- |
| [eudiw-authoritativ-sources-connector](apps/eudiw-authoritativ-sources-connector) | Authoritative Sources Connector | [README](apps/eudiw-authoritativ-sources-connector/README.md) |
| [eudiw-byob-service](apps/eudiw-byob-service) | Bring Your Own Briefcase Service | [README](apps/eudiw-byob-service/README.md) |
| [eudiw-oauth-server](apps/eudiw-oauth-server) | OAuth Server | [README](apps/eudiw-oauth-server/README.md) |
| eudiw-issuer-ui | Issuer UI (Bevisporten) | |
| [eudiw-verifier-demo](apps/eudiw-verifier-demo) | Verifier Demo | [README](apps/eudiw-verifier-demo/README.md) |
| [eudiw-issuer-server](apps/eudiw-issuer-server) | Issuer Server | [README](apps/eudiw-issuer-server/README.md) |
| [eudiw-status-list](apps/eudiw-status-list) | Status List Service | [README](apps/eudiw-status-list/README.md) |
| [eudiw-idporten-login](apps/eudiw-idporten-login) | Login via ID-porten til EUDI wallet | [README](apps/eudiw-idporten-login/README.md) |
| [eudiw-issuer-ui-demo](apps/eudiw-issuer-ui-demo) | Bevisgenerator for demo/testing | [README](apps/eudiw-issuer-ui-demo/README.md) |

## Struktur

```
apps/       Applikasjoner (én mappe per app)
docs/       Delt dokumentasjon på tvers av applikasjoner
```

Hver app har sin egen README med detaljer om oppsett, kjøring og testing.

## Lokal utvikling

Se individuell app sin README for spesifikke instruksjoner.
