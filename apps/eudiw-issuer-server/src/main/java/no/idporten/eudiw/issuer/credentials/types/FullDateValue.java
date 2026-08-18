package no.idporten.eudiw.issuer.credentials.types;

import java.time.LocalDate;
import java.time.ZonedDateTime;

// TODO verify if localdate should be removed
public record FullDateValue(LocalDate value) implements ClaimValue {
    public FullDateValue(ZonedDateTime value) {
        this(value != null ? value.toLocalDate() : null);
    }
}
