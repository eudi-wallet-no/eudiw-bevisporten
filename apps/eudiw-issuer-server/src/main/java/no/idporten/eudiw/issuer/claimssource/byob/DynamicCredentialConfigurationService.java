package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfigurations;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DynamicCredentialConfigurationService {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final ByobServiceIntegration byobServiceIntegration;

    private static final Logger log = LoggerFactory.getLogger(DynamicCredentialConfigurationService.class);

    //public static final String DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX = "net.eidas2sandkasse:";

    public DynamicCredentialConfigurationService(CredentialIssuerServerProperties credentialIssuerServerProperties, ByobServiceIntegration byobServiceIntegration) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.byobServiceIntegration = byobServiceIntegration;
    }

    // TODO: Add caching of the dynamic credential configurations to avoid multiple calls to byob-service on same request/create metadata
    // on start up does 32 calls to byob-service now. Worst-case 11 static claimsources x 2 + 2 dynamic claimssources * 4 + 6 unknown calles =22 + 8 + 6 = 36 . But 2 static claimsoures (advokat+førerkort) does not trigger calles, so 36-(2*2)=32
    private Map<String, DynamicCredentialConfiguration> getDynamicCredentialConfigurations() {
        DynamicCredentialConfigurations credentialConfigurations = byobServiceIntegration.retrieveAll();

        if(credentialConfigurations == null || credentialConfigurations.getCredentialConfigurations() == null || credentialConfigurations.getCredentialConfigurations().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, DynamicCredentialConfiguration> ccMap = new HashMap<>();
        for (DynamicCredentialConfiguration cc : credentialConfigurations.getCredentialConfigurations()) {
            ccMap.put(cc.credentialConfigurationId(), cc);
            log.info("Retrieved credential-configuration from byob-service: %s".formatted(cc.credentialConfigurationId()));
        }

        return ccMap;
    }

    public List<CredentialConfigurationProperties> generateCredentialConfigurations() {
        Map<String, DynamicCredentialConfiguration> configs = Collections.emptyMap();
        try {
            configs = getDynamicCredentialConfigurations();
        } catch (RuntimeException e) {
            log.error("Failed to fetch dynamic credential configurations from BYOB service. Continuing without BYOB credentials in metadata", e);
        }
        return configs.keySet().stream().map(this::generateCredentialConfiguration).toList();
    }

    protected CredentialConfigurationProperties generateCredentialConfiguration(String credentialConfigurationId) {
        CredentialConfigurationProperties dynamicCredentialConfigurationTemplate = credentialIssuerServerProperties.getDynamicCredentialConfigurationTemplate();
        DynamicCredentialConfiguration dynamicCredentialConfiguration = getDynamicCredentialConfigurations().get(credentialConfigurationId);
        CredentialConfigurationProperties credentialConfiguration = new CredentialConfigurationProperties();
        credentialConfiguration.setIdentifier(dynamicCredentialConfiguration.credentialConfigurationId());
        credentialConfiguration.setCredentialType(dynamicCredentialConfiguration.vct());
        credentialConfiguration.setFormat(CredentialFormat.fromString(dynamicCredentialConfiguration.format()));
        credentialConfiguration.setValidityDays(30);
        credentialConfiguration.setScope(dynamicCredentialConfigurationTemplate.getScope());
        credentialConfiguration.setGrantType(dynamicCredentialConfigurationTemplate.getGrantType());
        credentialConfiguration.setAuthorizationServer(dynamicCredentialConfigurationTemplate.getAuthorizationServer());
        credentialConfiguration.setPreAuthorizationServer(dynamicCredentialConfigurationTemplate.getPreAuthorizationServer());
        credentialConfiguration.setKeyStoreName(dynamicCredentialConfigurationTemplate.getKeyStoreName());
        return credentialConfiguration;
    }

    public DocumentMetadata getDocumentMetadataByCredentialType(String credentialType) {
        return getDynamicCredentialConfigurations().values().stream().filter(dcc -> credentialType.equals(dcc.vct())).findFirst().map(DynamicCredentialConfiguration::getCredentialMetadata).orElse(null);
    }

    public DocumentMetadata getDocumentMetadata(String credentialConfigurationId) {
        DocumentMetadata documentMetadata = getDynamicCredentialConfigurations().values().stream().filter(dcc -> credentialConfigurationId.equals(dcc.credentialConfigurationId())).findFirst().map(DynamicCredentialConfiguration::getCredentialMetadata).orElse(null);
        log.debug("Document metadata for credentialConfigurationId=%s /n %s".formatted(credentialConfigurationId, documentMetadata));
        return documentMetadata;
    }

}
