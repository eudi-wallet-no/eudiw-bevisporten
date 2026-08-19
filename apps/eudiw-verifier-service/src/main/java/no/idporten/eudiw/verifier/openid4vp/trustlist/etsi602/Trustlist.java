package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public record Trustlist(
        @JsonProperty("ListAndSchemeInformation")
        @Valid @NotNull ListAndSchemeInformation schemeInformation,
        @JsonProperty("TrustedEntitiesList")
        @Valid List<TrustedEntity> trustedEntitiesList) {
}
