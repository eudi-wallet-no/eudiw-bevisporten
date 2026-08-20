package no.idporten.eudiw.verifier.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;

import java.util.Objects;


public record TrustedEntityService(
        @JsonProperty("ServiceInformation")
        @Valid @NonNull ServiceInformation serviceInformation)
{
}
