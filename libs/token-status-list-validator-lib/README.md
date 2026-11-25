# token-status-list-validator-lib
EUDI wallet: token-status-list-validator-lib for validation of Token Status Lists.

## Requirements
- Java 25
- Maven
- kotlinx-coroutines 1.11.0 or newer at runtime
- walt.id's kotlinx-serialization fork `1.11.1-waltid-20260811` at runtime (from `https://maven.waltid.dev/releases`)

### Spring Boot applications
Spring Boot manages the kotlinx-coroutines and kotlinx-serialization versions. Both must be overridden in the
application's `pom.xml`:

```xml
<properties>
    <kotlin-coroutines.version>1.11.0</kotlin-coroutines.version>
    <kotlin-serialization.version>1.11.1-waltid-20260811</kotlin-serialization.version>
</properties>
```

If the application declares `kotlinx-serialization-bom` or `kotlinx-serialization-json-jvm` itself, use
`${kotlin-serialization.version}` there too, and add the walt.id repository:

```xml
<repository>
    <id>waltid-releases</id>
    <url>https://maven.waltid.dev/releases</url>
</repository>
```

Verify with `mvn dependency:tree -Dincludes='org.jetbrains.kotlinx:kotlinx-*'`.

## Configuration


## Usage

```java
TokenStatus status = validator.resolve(uri, idx);
status.isValid();           // status 0
status.format();            // JWT or CWT
status.signerCertificate(); // leaf from x5c/x5chain, check it against a trust list
```

A list not signed with an x5c/x5chain certificate (DID, well-known or jwk) gives `TokenStatusListException`.
