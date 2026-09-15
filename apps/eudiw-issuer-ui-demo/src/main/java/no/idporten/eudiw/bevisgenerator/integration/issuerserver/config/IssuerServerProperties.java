package no.idporten.eudiw.bevisgenerator.integration.issuerserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@EnableConfigurationProperties(IssuerServerProperties.class)
@ConfigurationProperties(prefix = "bevisgenerator.issuer-server")
public record IssuerServerProperties (
        String credentialIssuer,
        String issuanceEndpoint,
        List<CredentialConfiguration> credentialConfigurations,
        List<CredentialConfiguration> subjectCredentialConfigurations,
        List<CredentialConfiguration> additionalCredentialConfigurations,
        List<String> wellKnownUrls,
        List<CredentialConfiguration> allCredentialConfigurations) {

    @ConstructorBinding
    public IssuerServerProperties {}

    public IssuerServerProperties(String credentialIssuer,
                                  String issuanceEndpoint,
                                  List<CredentialConfiguration> credentialConfigurations,
                                  List<CredentialConfiguration> subjectCredentialConfigurations,
                                  List<CredentialConfiguration> additionalCredentialConfigurations,
                                  List<String> wellKnownUrls) {


        List<CredentialConfiguration> allCredentialConfigurations = concatCredentialDefinitions(List.of(credentialConfigurations, additionalCredentialConfigurations));

        this(
                credentialIssuer,
                issuanceEndpoint,
                credentialConfigurations,
                subjectCredentialConfigurations,
                additionalCredentialConfigurations,
                wellKnownUrls,
                allCredentialConfigurations
        );
    }

    public String getIssuanceEndpoint() {
        return issuanceEndpoint();
    }

    public String getIssuanceUrl() {
        return credentialIssuer() + issuanceEndpoint();
    }

    public CredentialConfiguration findCredentialConfiguration(String selectionId) {
        if (allCredentialConfigurations() == null) {
            return null;
        }

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
        if (allCredentialConfigurations() == null) {
            return null;
        }

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

    private static List<CredentialConfiguration> concatCredentialDefinitions(List<List<CredentialConfiguration>> credentialConfigurations) {
        List<CredentialConfiguration> allCredentialConfigurations = new ArrayList<>();

        for  (List<CredentialConfiguration> credentialConfiguration : credentialConfigurations) {
            if (credentialConfiguration != null) {
                allCredentialConfigurations.addAll(credentialConfiguration);
            }
        }

        return allCredentialConfigurations;
    }
}
