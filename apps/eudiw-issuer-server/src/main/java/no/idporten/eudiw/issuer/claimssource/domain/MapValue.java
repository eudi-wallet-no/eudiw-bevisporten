package no.idporten.eudiw.issuer.claimssource.domain;

import java.util.Map;

public record MapValue(Map<String, ClaimValue> value) implements ClaimValue {

}
