package no.idporten.eudiw.issuer.claimssource.domain;

import java.util.List;

public record ListValue(List<ClaimValue> value) implements ClaimValue {

}
