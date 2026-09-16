package no.idporten.eudiw.issuer.ui.issuer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "issuer-ui.issuer-server")
public class IssuerServerProperties {


    private String baseUrl;
    private String issuanceEndpoint;
    private List<CredentialConfiguration> credentialConfigurations;
    private List<CredentialConfiguration> additionalCredentialConfigurations;

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

    public void setCredentialConfigurations(List<CredentialConfiguration> credentialConfigurations) {
        this.credentialConfigurations = credentialConfigurations;
    }

    public List<CredentialConfiguration> getCredentialConfigurations() {
        return credentialConfigurations;
    }

    public List<CredentialConfiguration> getAllCredentialConfigurations() {
        List <CredentialConfiguration> allCredentialConfigurations = new ArrayList<>();
        if (credentialConfigurations != null) {
            allCredentialConfigurations.addAll(credentialConfigurations);
        }
        if (additionalCredentialConfigurations != null) {
            allCredentialConfigurations.addAll(additionalCredentialConfigurations);
        }
        return allCredentialConfigurations;
    }

    public CredentialConfiguration findCredentialConfiguration(String credentialConfigurationId) {
        return getAllCredentialConfigurations()
                .stream()
                .filter(credentialConfiguration -> credentialConfiguration.id().equals(credentialConfigurationId))
                .findFirst()
                .orElse(null);
    }

    public List<CredentialConfiguration> getAdditionalCredentialConfigurations() {
        return additionalCredentialConfigurations;
    }

    public void setAdditionalCredentialConfigurations(List<CredentialConfiguration> additionalCredentialConfigurations) {
        this.additionalCredentialConfigurations = additionalCredentialConfigurations;
    }
}
