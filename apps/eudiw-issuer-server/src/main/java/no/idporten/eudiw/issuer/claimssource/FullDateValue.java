package no.idporten.eudiw.issuer.claimssource;

import java.time.LocalDate;

public record FullDateValue(LocalDate value) implements ClaimValue {

}
