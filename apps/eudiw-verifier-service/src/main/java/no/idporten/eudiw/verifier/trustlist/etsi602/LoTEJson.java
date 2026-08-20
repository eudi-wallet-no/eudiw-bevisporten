package no.idporten.eudiw.verifier.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize
public record LoTEJson(
        @JsonProperty("LoTE")
        @NotNull Trustlist lote
) {
}
