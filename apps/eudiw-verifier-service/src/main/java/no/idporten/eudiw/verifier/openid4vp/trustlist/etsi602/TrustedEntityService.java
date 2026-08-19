package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;



public record TrustedEntityService(
        @JsonProperty("ServiceInformation")
        @Valid @NonNull ServiceInformation serviceInformation) {
}
