package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.byob.DynamicCredentialConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Credential configurations supported by issuer.  May contain static or dynamic credential configurations.
 */
@Service
public class CredentialConfigurationService implements InitializingBean {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final DynamicCredentialConfigurationService dynamicCredentialConfigurationService;

    private List<CredentialConfigurationProperties> staticCredentialConfigurations;
    private volatile List<CredentialConfigurationProperties> dynamicCredentialConfigurations;

    public CredentialConfigurationService(CredentialIssuerServerProperties credentialIssuerServerProperties, DynamicCredentialConfigurationService dynamicCredentialConfigurationService) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.dynamicCredentialConfigurationService = dynamicCredentialConfigurationService;
    }

    private CredentialConfigurationProperties findCredentialConfiguration(String credentialIdentifier, List<CredentialConfigurationProperties> credentialConfigurations) {
        return credentialConfigurations.stream()
                .filter(credentialConfigurationProperties -> Objects.equals(credentialIdentifier, credentialConfigurationProperties.getIdentifier()))
                .findFirst()
                .orElse(null);
    }

    public CredentialConfigurationProperties findCredentialConfiguration(String credentialIdentifier) {
        CredentialConfigurationProperties credentialConfiguration = findCredentialConfiguration(credentialIdentifier, staticCredentialConfigurations);
        if (credentialConfiguration == null) {
            updateCredentialConfigurations();
            credentialConfiguration = findCredentialConfiguration(credentialIdentifier, dynamicCredentialConfigurations);
        }
        if (credentialConfiguration == null) {
            throw new IssuerServerException("unknown_credential_identifier", "Unknown credential identifier.", HttpStatus.BAD_REQUEST);
        }
        return credentialConfiguration;
    }

    private void updateCredentialConfigurations() {
        this.dynamicCredentialConfigurations = dynamicCredentialConfigurationService.generateCredentialConfigurations();
    }

    public boolean isStaticCredentialConfiguration(String credentialIdentifier) {
        return findCredentialConfiguration(credentialIdentifier, staticCredentialConfigurations) != null;
    }

    @Override
    public void afterPropertiesSet() {
        this.staticCredentialConfigurations = Collections.unmodifiableList(credentialIssuerServerProperties.getCredentialConfigurations());
        this.dynamicCredentialConfigurations = new ArrayList<>(dynamicCredentialConfigurationService.generateCredentialConfigurations());
    }

}
