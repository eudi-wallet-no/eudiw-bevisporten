package no.idporten.eudiw.issuer.config;

import org.springframework.stereotype.Service;

/**
 * Credential configurations supported by issuer.
 */
@Service
public class CredentialConfigurationService {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    public CredentialConfigurationService(CredentialIssuerServerProperties credentialIssuerServerProperties) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
    }

    public ExtendedCredentialConfiguration findCredentialConfiguration(String credentialIdentifier) {
        return credentialIssuerServerProperties.findCredentialConfiguration(credentialIdentifier);
    }

}
