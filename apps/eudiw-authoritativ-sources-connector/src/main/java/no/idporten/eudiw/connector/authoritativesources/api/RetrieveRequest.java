package no.idporten.eudiw.connector.authoritativesources.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RetrieveRequest(
        @JsonProperty("subject")
        @NotNull
        @Valid
        Subject subject,

        @JsonProperty("credential_type")
        @NotEmpty(message = "Credential type is required")
        @Valid
        String credentialType
) {
}
