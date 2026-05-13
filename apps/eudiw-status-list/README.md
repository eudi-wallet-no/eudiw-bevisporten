# eudiw-status-list
EUDIW Token Status List for eidas2sandkasse in Norway.

## Requirements
- Java 25
- Maven
- Docker

## Configuration

Profiles in the [resources](/src/main/resources) folder:

| Profile | Description                                |
|---------|--------------------------------------------|
| dev     | Local development + mariadb in docker      |
| docker  | Docker locally, run by docker-compose file |
| systest | Systest environment                        |
| test    | Test environment                           |


## Running the application locally

### dev
This application requires a MariaDB instance to run. Start it with:

```bash
docker compose up -d status-list-db
```

The local hosts file should include:
```
127.0.0.1 status-list
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

### docker

The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://status-list:9288.
