package no.idporten.eudiw.verifier.config;

import jakarta.validation.constraints.NotNull;

import java.net.URI;

public record TrustlistContent(
        @NotNull URI pid,
        @NotNull URI attestations
) {
}
