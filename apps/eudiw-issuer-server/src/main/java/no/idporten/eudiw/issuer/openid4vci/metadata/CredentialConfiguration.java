package no.idporten.eudiw.issuer.openid4vci.metadata;

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
public class CredentialConfiguration  {

    @JsonProperty("doctype")
    private String doctype;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("format")
    private String format;

    @JsonProperty("cryptographic_binding_methods_supported")
    private List<String> cryptographicBindingMethods;

    @JsonProperty("display")
    private List<Display> display;

    @JsonProperty("claims")
    private List<ClaimsDescription> claims;

    @JsonProperty("proof_types_supported")
    private ProofTypes proofTypes;

}
