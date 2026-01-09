package no.idporten.eudiw.byob.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialMetadata (
        @JsonProperty("displays")
        List<Display> displays,
        @JsonProperty("claims")
        List<Claims> claims
) {
}
