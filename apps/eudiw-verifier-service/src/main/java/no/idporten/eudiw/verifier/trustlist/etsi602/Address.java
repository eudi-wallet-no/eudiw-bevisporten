package no.idporten.eudiw.verifier.trustlist.etsi602;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public record Address(@NotBlank String streetAddress, @NotBlank String postalCode, @NotBlank String locality, @NotBlank String country) {
}

