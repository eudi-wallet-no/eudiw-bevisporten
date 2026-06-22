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

The local hosts file should include:
```
127.0.0.1 status-list
```

## Maven

Start required dependencies with Docker Compose.
```
docker-compose up --scale status-list=0 -d
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will run on http://status-list:9288 with the `dev` profile.

## Docker

The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://status-list:9288 with the `docker` profile.
