package no.idporten.eudiw.issuer.ui.issuer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "issuer-ui.issuer-server")
public class IssuerServerProperties {


    private String baseUrl;
    private String issuanceEndpoint;
    private Map<String, CredentialConfiguration> credentialConfigurations;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getIssuanceEndpoint() {
        return issuanceEndpoint;
    }

    public void setIssuanceEndpoint(String issuanceEndpoint) {
        this.issuanceEndpoint = issuanceEndpoint;
    }

    public String getIssuanceUrl() {
        return baseUrl + issuanceEndpoint;
    }

    public void setCredentialConfigurations(Map<String, CredentialConfiguration> credentialConfigurations) {
        this.credentialConfigurations = credentialConfigurations;
    }

    public List<CredentialConfiguration> getCredentialConfigurations() {
        return credentialConfigurations.values().stream().toList();
    }

    public CredentialConfiguration findCredentialConfiguration(String credentialConfigurationId) {
        return credentialConfigurations
                .values()
                .stream()
                .filter(credentialConfiguration -> credentialConfiguration.id().equals(credentialConfigurationId))
                .findFirst()
                .orElse(null);
    }
}
