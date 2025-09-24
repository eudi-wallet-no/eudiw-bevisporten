package no.idporten.eudiw.issuer.ui.demo.issuer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;
import java.util.Objects;

@EnableConfigurationProperties(IssuerServerProperties.class)
@ConfigurationProperties(prefix = "issuer-ui-demo.issuer-server")
public record IssuerServerProperties (String baseUrl, String issuanceEndpoint, List<CredentialConfiguration> credentialConfigurations) {

    public String credentialIssuer() {
        return baseUrl();
    }

    public String getIssuanceEndpoint() {
        return issuanceEndpoint();
    }

    public String getIssuanceUrl() {
        return baseUrl() + issuanceEndpoint();
    }

    public CredentialConfiguration findCredentialConfiguration(String credentialConfigurationId) {
        return credentialConfigurations()
                .stream()
                .filter(credentialConfiguration -> Objects.equals(credentialConfigurationId, credentialConfiguration.credentialConfigurationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown credential configuration id"));
    }

}
