# eudiw-issuer-ui-demo

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDI wallet: Bevisgenerator for pre-authorized flow.

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
| systest-local | Local application against systest        |
| test    | Test environment                           |

## Secrets
Clone https://github.com/eudi-wallet-no/eudiw-developer-secrets and follow the instructions in the README.
For the `systest-local` profile, fill in the placeholders in `secrets/eudiw-issuer-ui-demo-systest.env`.

## Running the application locally

The `dev` and `docker` profiles run the application with similar configuration.

The local hosts file should include:
```
127.0.0.1 bevisgenerator
```

For the `dev` and `docker` profiles, this application depends on `byob-service` and `issuer-server`. Start those first from `eudiw-issuer-server`:
```
cd ../eudiw-issuer-server
docker-compose up --scale issuer-ui-demo=0 --scale issuer-ui=0 -d
```

To run the application locally against the systest dependencies, do not start the local Docker services. Start the `systest-local` profile with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=systest-local
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

The application can be started with Docker compose:
```
docker-compose up --build
```

To automatically rebuild and restart the container when Thymeleaf templates or static CSS/HTML change, use watch mode instead:
```
docker-compose watch
```

The application will run on http://bevisgenerator:9290.
