package no.idporten.eudiw.verifier.trustlist.etsi602;

import jakarta.validation.constraints.NotBlank;

public record InformationUri(
        @NotBlank String a,
        String b,
        @NotBlank String c
) {
}
