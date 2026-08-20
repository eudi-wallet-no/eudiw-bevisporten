package no.idporten.eudiw.verifier.trustlist.etsi602;


import jakarta.validation.constraints.NotBlank;

public record ServiceName(
        @NotBlank String langNo,
        @NotBlank String langEn
) {
}
