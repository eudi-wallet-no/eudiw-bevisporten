package no.idporten.eudiw.bevisgenerator.integration.issuerserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@EnableConfigurationProperties(IssuerServerProperties.class)
@ConfigurationProperties(prefix = "bevisgenerator.issuer-server")
public record IssuerServerProperties(
        String credentialIssuer,
        String issuanceEndpoint,
        List<CredentialConfiguration> credentialConfigurations,
        List<CredentialConfiguration> subjectCredentialConfigurations,
        List<CredentialConfiguration> additionalCredentialConfigurations,
        List<String> wellKnownUrls,
        List<String> excludedCredentialConfigurations
) {

    public IssuerServerProperties(
            String credentialIssuer,
            String issuanceEndpoint,
            List<CredentialConfiguration> credentialConfigurations,
            List<CredentialConfiguration> subjectCredentialConfigurations,
            List<CredentialConfiguration> additionalCredentialConfigurations,
            List<String> wellKnownUrls
    ) {
        this(
                credentialIssuer,
                issuanceEndpoint,
                credentialConfigurations,
                subjectCredentialConfigurations,
                additionalCredentialConfigurations,
                wellKnownUrls,
                List.of()
        );
    }

    public List<CredentialConfiguration> allCredentialConfigurations() {
        Set<String> excludedSelectionIds =
                excludedCredentialConfigurations == null ? Set.of() : Set.copyOf(excludedCredentialConfigurations);
        List<CredentialConfiguration> configurations = new ArrayList<>();

        if (credentialConfigurations != null) {
            configurations.addAll(credentialConfigurations);
        }
        if (additionalCredentialConfigurations != null) {
            configurations.addAll(additionalCredentialConfigurations);
        }

        return configurations.stream()
                .filter(credentialConfiguration -> !excludedSelectionIds.contains(credentialConfiguration.selectionId()))
                .toList();
    }

    public String getIssuanceEndpoint() {
        return issuanceEndpoint();
    }

    public String getIssuanceUrl() {
        return credentialIssuer() + issuanceEndpoint();
    }

    public CredentialConfiguration findCredentialConfiguration(String selectionId) {
        return allCredentialConfigurations()
                .stream()
                .filter(credentialConfiguration -> Objects.equals(selectionId, credentialConfiguration.selectionId()))
                .findFirst()
                .orElse(null);
    }

    public CredentialConfiguration findCredentialConfiguration(
            String credentialIssuer,
            String credentialConfigurationId
    ) {
        return allCredentialConfigurations()
                .stream()
                .filter(credentialConfiguration ->
                        Objects.equals(credentialIssuer, credentialConfiguration.credentialIssuer())
                                && Objects.equals(
                                        credentialConfigurationId,
                                        credentialConfiguration.credentialConfigurationId()
                                )
                )
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
