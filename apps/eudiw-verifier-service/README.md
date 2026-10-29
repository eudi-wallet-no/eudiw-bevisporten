# eudiw-verifier-backend
EUDI wallet: verifier-service for eidas2sandkasse.

## Requirements
- Java 25
- Maven
- Docker

## Configuration

Profiles in the [resources](/src/main/resources) folder:

| Profile | Description                                |
|---------|--------------------------------------------|
| dev     | Local development                          |
| docker  | Docker locally, run by docker-compose file |
| systest | Systest environment                        |
| test    | Test environment                           |

### Empty JSON trustlists

An ETSI TS 119 602 JSON trustlist with a missing, null or empty
`TrustedEntitiesList` is treated as having no certificate matches. The verifier
logs a warning and continues checking the remaining configured trustlists. If
none of them match, trustlist validation returns `INVALID`.


## Secrets
Clone https://github.com/eudi-wallet-no/eudiw-developer-secrets and follow the instructions in the README.

## Running the application locally

The `dev` and `docker` profiles runs the application with similar configuration.

The local hosts file should include:
```
127.0.0.1 verifier-service
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://verifier-service:9285.
