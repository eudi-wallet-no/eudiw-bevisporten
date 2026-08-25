package no.idporten.eudiw.verifier.trustlist.etsi602;

import jakarta.validation.constraints.NotBlank;

public record TeAddress(
        @NotBlank String phoneNumber
) {

}
