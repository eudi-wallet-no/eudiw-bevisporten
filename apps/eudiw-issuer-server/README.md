# eudiw-issuer-server

> [!INFO]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Credential Issuer Server.
Digdir generic credential issuer server for issuing verifiable credentials to digital wallets in eidas2sandkasse.

## Requirements
- Java 25
- Maven
- Docker

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

## Configuration

Profiles in the [resources](/src/main/resources) folder:

| Profile | Description                                |
|---------|--------------------------------------------|
| dev     | Local development                          |
| docker  | Docker locally, run by docker-compose file |
| systest | Systest environment                        |
| test    | Test environment                           |


## Secrets

Clone https://github.com/eudi-wallet-no/eudiw-developer-secrets and follow the instructions in the README.

## Running the application locally

The `dev` and `docker` profiles runs the application with similar configuration.

The local hosts file should include:
```
127.0.0.1 issuer-server oauth-server byob-service authoritative-sources-connector status-list
```

## Maven

Run requirements with Docker
```
docker-compose up --scale issuer-server=0 -d
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will run on http://localhost:9240 with the `dev` profile.

## Docker
The application and requirements can be started with Docker compose
```
docker-compose up --build
```

The application will run on http://issuer-server:9240 with the `docker` profile.
