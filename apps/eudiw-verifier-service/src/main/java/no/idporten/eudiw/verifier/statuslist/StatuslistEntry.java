package no.idporten.eudiw.verifier.statuslist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.net.URI;

public record StatuslistEntry(
        @NotBlank String idx,
        @NotNull URI uri
) {
}