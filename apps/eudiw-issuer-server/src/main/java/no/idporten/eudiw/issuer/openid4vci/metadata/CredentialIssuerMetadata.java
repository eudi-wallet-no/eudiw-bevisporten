package no.idporten.eudiw.issuer.openid4vci.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.net.URI;
import java.util.List;

/**
 * Credential Issuer Metadata - https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-issuer-metadata
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialIssuerMetadata {

    @JsonProperty("credential_issuer")
    private URI credentialIssuer;

    @JsonProperty("authorization_servers")
    private List<URI> authorizationServers;

    @JsonProperty("credential_endpoint")
    private URI credentialEndpoint;

    @JsonProperty("nonce_endpoint")
    private URI nonceEndpoint;

    @JsonProperty("notification_endpoint")
    private URI notificationEndpoint;

    @JsonProperty("credential_configurations_supported")
    private CredentialConfigurations credentialConfigurations;

    @Singular("display")
    @JsonProperty("display")
    private List<Display> displays;

}
