package no.idporten.eudiw.byob.service.model.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


@JsonIgnoreProperties(ignoreUnknown = true)

public record CredentialConfigurationRequestResource(

        @JsonProperty("credential_type")
        @Schema(description = "Credential type", example = "my_credential")
        @Pattern(regexp = "^[a-z0-9_:.]{3,155}$",
                message = "Credential type must be lowercase letters/numbers, underscores or colons, length 3-155")
        String credentialType,

        @JsonProperty("format")
        @Schema(description = "Credential format, either dc+sd-jwt or mso_mdoc", example = "dc+sd-jwt")
        @Pattern(regexp = "dc\\+sd-jwt|mso_mdoc", message = "Format must be dc+sd-jwt or mso_mdoc")
        String format,

        @NotBlank(message = "scope must have a value")
        @JsonProperty("scope")
        @Schema(description = "Scope used to issue credential", example = "eudiw:eidas2sandkasse:dynamicvc")
        @Pattern(regexp = "^[a-zæøå][a-zæøå0-9+\\-.]*:([a-zæøå0-9]+/?)+(:[a-zæøå0-9]+)*[a-zæøå0-9]+(\\.[a-zæøå0-9]+)*$", message = "Scope must be on format prefix:sub_scope") // fra https://github.com/felleslosninger/kut-selvbetjening-api/blob/main/src/main/java/no/digdir/kundetjenester/selvbetjening/api/scope/ScopeValidator.java
        String scope,

        @Valid
        @Schema(description = "Example credential data")
        @JsonProperty("example_credential_data")
        ExampleCredentialDataRequestResource exampleCredentialData,

        @Valid
        @JsonProperty("credential_metadata")
        @Schema(description = "Credential metadata, including display information and claims information")
        @NotNull(message = "credential_metadata is null")
        CredentialMetadataRequestResource credentialMetadata
) {
}
