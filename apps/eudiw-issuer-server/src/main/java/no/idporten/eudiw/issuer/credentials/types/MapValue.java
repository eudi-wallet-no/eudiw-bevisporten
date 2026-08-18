package no.idporten.eudiw.issuer.credentials.types;

import java.util.Map;

public record MapValue(Map<String, ClaimValue> value) implements ClaimValue {

}
