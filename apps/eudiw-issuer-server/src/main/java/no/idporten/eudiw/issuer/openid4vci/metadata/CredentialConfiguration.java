package no.idporten.eudiw.issuer.openid4vci.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Credential configuration metadata built from issuer config, credential properties and claims sources.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialConfiguration  {

    @JsonProperty("doctype")
    private String doctype;

    @JsonProperty("vct")
    private String vct;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("format")
    private String format;

    @JsonProperty("cryptographic_binding_methods_supported")
    private List<String> cryptographicBindingMethods;

    @JsonProperty("credential_signing_alg_values_supported")
    private List<String> credentialSigningAlgValuesSupported;

    @JsonProperty("display")
    private List<Display> display;

    @JsonProperty("claims")
    private List<ClaimsDescription> claims;

    @JsonProperty("proof_types_supported")
    private ProofTypes proofTypes;

}
