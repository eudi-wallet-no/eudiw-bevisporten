package no.idporten.eudiw.issuer.openid4vci;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.net.URI;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialIssuerMetadata {

    @JsonProperty("credential_issuer")
    private URI credentialIssuer;

    @JsonProperty("credential_endpoint")
    private URI credentialEndpoint;

    @JsonProperty("credential_configurations_supported")
    private CredentialConfigurations credentialConfigurations;

}
