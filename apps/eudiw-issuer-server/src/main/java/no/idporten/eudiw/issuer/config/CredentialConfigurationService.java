package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.byob.DynamicCredentialConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static no.idporten.eudiw.issuer.claimssource.byob.ByobClaimsSource.DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX;

/**
 * Credential configurations supported by issuer.  May contain static or dynamic credential configurations.
 */
@Service
public class CredentialConfigurationService implements InitializingBean {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final DynamicCredentialConfigurationService dynamicCredentialConfigurationService;

    private List<CredentialConfigurationProperties> credentialConfigurations;


    public CredentialConfigurationService(CredentialIssuerServerProperties credentialIssuerServerProperties, DynamicCredentialConfigurationService dynamicCredentialConfigurationService) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.dynamicCredentialConfigurationService = dynamicCredentialConfigurationService;
    }

    public CredentialConfigurationProperties findCredentialConfiguration(String credentialIdentifier) {
        updateCredentialConfigurations();
        return credentialConfigurations.stream()
                .filter(credentialConfigurationProperties -> Objects.equals(credentialIdentifier, credentialConfigurationProperties.getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("unknown_credential_identifier", "Unknown credential identifier.", HttpStatus.BAD_REQUEST));
    }

    private void updateCredentialConfigurations() {
        List<CredentialConfigurationProperties> ccByob = dynamicCredentialConfigurationService.generateCredentialConfigurations();
        List<CredentialConfigurationProperties> toRemove = new ArrayList<>();
        for (CredentialConfigurationProperties cc : credentialConfigurations) {
            String existingCCId = cc.getIdentifier();
            if (!existingCCId.startsWith(DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX)) {
                continue;
            }
            if (ccByob.stream().filter(c -> c.getIdentifier().equals(existingCCId)).findFirst().isEmpty()) {
                toRemove.add(cc);
            }
        }
        credentialConfigurations.removeAll(toRemove);
        this.credentialConfigurations.addAll(ccByob);
    }


    @Override
    public void afterPropertiesSet() {
        List<CredentialConfigurationProperties> allCredentialConfigurations = new ArrayList<>();
        allCredentialConfigurations.addAll(credentialIssuerServerProperties.getCredentialConfigurations());
        allCredentialConfigurations.addAll(dynamicCredentialConfigurationService.generateCredentialConfigurations());
        this.credentialConfigurations = allCredentialConfigurations;
    }

}
