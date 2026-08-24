package no.idporten.eudiw.verifier.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public record Trustlist(
        @JsonProperty("ListAndSchemeInformation")
        @Valid @NotNull ListAndSchemeInformation schemeInformation,
        @JsonProperty("TrustedEntitiesList")
        @Valid @NotEmpty List<@NotNull TrustedEntity> trustedEntitiesList) {
}
