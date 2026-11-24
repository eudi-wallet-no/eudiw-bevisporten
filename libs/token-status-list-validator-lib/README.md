# token-status-list-validator-lib
EUDI wallet: token-status-list-validator-lib for validation of Token Status Lists.

## Requirements
- Java 25
- Maven
- kotlinx-coroutines 1.11.0 or newer at runtime

### Spring Boot applications
Spring Boot manages the kotlinx-coroutines version and downgrades it (Spring Boot 4.1.2 uses 1.10.2).
The library and walt.id 1.2.x are compiled against 1.11.0, so validation fails at runtime with:

```
java.lang.NoSuchMethodError: 'java.lang.Object kotlinx.coroutines.BuildersKt.runBlockingK$default(...)'
```

Override the Spring Boot managed version in the application's `pom.xml`:

```xml
<properties>
    <kotlin-coroutines.version>1.11.0</kotlin-coroutines.version>
</properties>
```

Verify with `mvn dependency:tree -Dincludes=org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm`.

## Configuration


## Usage


