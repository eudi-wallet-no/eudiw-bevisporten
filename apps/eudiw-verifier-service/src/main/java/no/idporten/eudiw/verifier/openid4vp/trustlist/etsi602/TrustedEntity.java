package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;


public record TrustedEntity(
        @JsonProperty("TrustedEntityInformation")
        TrustedEntityInformation trustedEntityInformation,
        @JsonProperty("TrustedEntityServices")
        @Valid List<TrustedEntityService> trustedEntityServices) {
}
