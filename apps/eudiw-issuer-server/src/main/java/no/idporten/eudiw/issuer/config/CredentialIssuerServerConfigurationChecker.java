package no.idporten.eudiw.issuer.config;

import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * Some sanity checks on credential issuer server configuration on startup.
 */
@Service
public class CredentialIssuerServerConfigurationChecker implements InitializingBean {

    private final KeystoreManager keystoreManager;
    private final CredentialIssuerServerProperties credentialIssuerProperties;

    public CredentialIssuerServerConfigurationChecker(KeystoreManager keystoreManager, CredentialIssuerServerProperties credentialIssuerProperties) {
        this.keystoreManager = keystoreManager;
        this.credentialIssuerProperties = credentialIssuerProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (CredentialIssuerTenant credentialIssuerTenant : credentialIssuerProperties.getTenants().values()) {
            if (credentialIssuerTenant.canSignCredentialIssuerMetadata()) {
                keystoreManager.getKeystore(credentialIssuerTenant.getMetadataSigningKeystore());
            }
        }
    }

}
