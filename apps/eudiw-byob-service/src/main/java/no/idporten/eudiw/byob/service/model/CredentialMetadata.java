package no.idporten.eudiw.byob.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialMetadata (
        @Valid
        @JsonProperty("display")
        @NotEmpty(message = "CredentialMetadata display is null or empty")
        List<Display> display,

        @Valid
        @NotEmpty(message = "CredentialMetadata claims is null or empty")
        @JsonProperty("claims")
        List<Claims> claims
) {
}
