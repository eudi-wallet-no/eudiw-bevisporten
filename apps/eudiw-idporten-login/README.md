# eudiw-idporten-login

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW ID-porten login authenticates Norwegian and European users through OpenID 4 Verifiable Presentation of EUDI Wallet PID documents.

The application is an OpenID Connect Provider implementing the OIDC profile for ID-portens internal login orchestration.

The EUDIW verifier service is used to handle the OpenID4VP protocol with wallets.  This application handles the browser interaction same device and cross device presentation flows.

## Requirements
- Java 25
- Maven
- Docker
- Redis

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

## Configuration

Profiles in the [resources](/src/main/resources) folder:

| Profile | Description                                   |
|---------|-----------------------------------------------|
| dev     | Local development                             |
| docker  | Docker locally, run by docker-compose file    |
| systest | Systest environment                           |
| test    | Test environment                              |


## Running the application locally

Redis is needed for HTTP session and cache of protocol objects.  The docker compose setup adds a Redis server.

The `dev` and `docker` profiles runs the application with the same configuration (certs, url).

The local hosts file should include:
```
127.0.0.1 eudiw-idporten-login
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://eudiw-idporten-login:9283/ .
An OIDC test client can be found at http://localhost:8888/ .
