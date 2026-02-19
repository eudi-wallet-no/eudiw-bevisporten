package no.idporten.eudiw.byob.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;


@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialConfiguration(

        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,

        @JsonProperty("credential_type")
        @Pattern(regexp = "^[a-z0-9_:.]{3,255}$",
                message = "Credential type kan ikke ha spesielle karakterer eller symboler")
        String credentialType,

        @JsonProperty("format")
        String format,

        @JsonProperty("scope")
        String scope,

        @JsonProperty("example_credential_data")
        ExampleCredentialData exampleCredentialData,

        @JsonProperty("credential_metadata")
        CredentialMetadata credentialMetadata
) {
}
