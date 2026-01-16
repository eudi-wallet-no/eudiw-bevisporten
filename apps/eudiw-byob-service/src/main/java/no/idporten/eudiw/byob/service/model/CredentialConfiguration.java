package no.idporten.eudiw.byob.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialConfiguration(

        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,

        @JsonProperty("vct")
        @Pattern(regexp = "^[a-z0-9_:.]{3,255}$",
                message = "vct kan ikke ha spesielle karakterer eller symboler")
        String vct,

        @JsonProperty("format")
        String format,

        @JsonProperty("example_credential_data")
        List<ExampleCredentialData> exampleCredentialData,

        @JsonProperty("credential_metadata")
        CredentialMetadata credentialMetadata
) {
}
