package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotNull;

public record AuthoritativeSourceProperties(@NotNull APIConnectionProperties api) {
}
