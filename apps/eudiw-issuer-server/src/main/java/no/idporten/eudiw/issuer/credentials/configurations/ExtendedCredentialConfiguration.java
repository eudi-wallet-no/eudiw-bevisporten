package no.idporten.eudiw.issuer.credentials.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;
import org.springframework.validation.annotation.Validated;

@JsonIgnoreProperties(ignoreUnknown = true)
@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ExtendedCredentialConfiguration {

    /**
     * Credential identifier used in metadata, requests, responses
     */
    @JsonProperty("credential_configuration_id")
    @NotEmpty
    private String credentialConfigurationId;

    /**
     * Document type used in metadata.
     */
    @JsonProperty("credential_type")
    @NotNull
    private String credentialType;

    /**
     * Credential format
     */
    @NotNull
    @JsonProperty("format")
    private CredentialFormat format;

    /**
     * Scope required in access_token at the credentials endpoint
     */
    @NotNull
    @JsonProperty("scope")
    private String scope;

    /**
     * Extended credential metadata including data types.
     */
    @NotNull
    @JsonProperty("credential_metadata")
    private ExtendedCredentialMetadata extendedCredentialMetadata;

    /**
     * Credential issuer context containing information about how to issue this credential.
     */
    @JsonProperty("credential_issuer_context")
    private CredentialIssuerContext credentialIssuerContext;

}
