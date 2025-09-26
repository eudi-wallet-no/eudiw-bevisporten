package no.idporten.eudiw.issuer.claimssource.domain;

import java.time.LocalDate;

public record FullDateValue(LocalDate value) implements ClaimValue {

}
