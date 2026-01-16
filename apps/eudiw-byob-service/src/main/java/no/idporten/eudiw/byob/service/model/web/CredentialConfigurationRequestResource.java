package no.idporten.eudiw.byob.service.model.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialConfigurationRequestResource(

        @JsonProperty("vct")
        @Pattern(regexp = "^[a-z0-9_:.]{3,155}$",
                message = "vct must be lowercase letters/numbers, underscores or colons, length 3-155")
        String vct,

        @JsonProperty("format")
        @Pattern(regexp = "dc\\+sd-jwt", message = "Format must be dc+sd-jwt")
        String format,

        @Valid
        @JsonProperty("example_credential_data")
        List<ExampleCredentialDataRequestResource> exampleCredentialData,

        @Valid
        @JsonProperty("credential_metadata")
        @NotNull(message = "credential_metadata is null")
        CredentialMetadataRequestResource credentialMetadata
) {
}
