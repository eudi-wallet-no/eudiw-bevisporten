package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602;

import jakarta.validation.constraints.NotBlank;

public record TSName(@NotBlank String langNo, @NotBlank String langEn) {
}
