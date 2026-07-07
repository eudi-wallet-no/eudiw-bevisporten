package no.idporten.eudiw.bevisgenerator.integration.issuerserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;
import java.util.Objects;

@EnableConfigurationProperties(IssuerServerProperties.class)
@ConfigurationProperties(prefix = "bevisgenerator.issuer-server")
public record IssuerServerProperties (String credentialIssuer,
                                      String issuanceEndpoint,
                                      List<CredentialConfiguration> credentialConfigurations,
                                      List<CredentialConfiguration> subjectCredentialConfigurations) {

    public String getIssuanceEndpoint() {
        return issuanceEndpoint();
    }

    public String getIssuanceUrl() {
        return credentialIssuer() + issuanceEndpoint();
    }

    public CredentialConfiguration findCredentialConfiguration(String credentialConfigurationId) {
        if (credentialConfigurations() == null) {
            return null;
        }

        return credentialConfigurations()
                .stream()
                .filter(credentialConfiguration -> Objects.equals(credentialConfigurationId, credentialConfiguration.credentialConfigurationId()))
                .findFirst()
                .orElse(null);
    }

    public CredentialConfiguration findSubjectCredentialConfigurationById(String credentialConfigurationId) {
        if (subjectCredentialConfigurations() == null) {
            return null;
        }

        return subjectCredentialConfigurations()
                .stream()
                .filter(credentialConfiguration -> Objects.equals(credentialConfigurationId, credentialConfiguration.credentialConfigurationId()))
                .findFirst()
                .orElse(null);
    }
}
