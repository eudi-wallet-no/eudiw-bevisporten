package no.idporten.eudiw.issuer.credentials.types;

import java.util.List;

public record ListValue(List<ClaimValue> value) implements ClaimValue {

}
