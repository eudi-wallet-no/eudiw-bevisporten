package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfigurations;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;
import no.idporten.eudiw.issuer.openid4vci.metadata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DynamicCredentialConfigurationService {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final ByobServiceIntegration byobServiceIntegration;

    private static final Logger log = LoggerFactory.getLogger(DynamicCredentialConfigurationService.class);

    public DynamicCredentialConfigurationService(CredentialIssuerServerProperties credentialIssuerServerProperties, ByobServiceIntegration byobServiceIntegration) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.byobServiceIntegration = byobServiceIntegration;
    }

    // TODO: Add caching of the dynamic credential configurations to avoid multiple calls to byob-service on same request/create metadata
    public Map<String, DynamicCredentialConfiguration> getDynamicCredentialConfigurations() {
        DynamicCredentialConfigurations credentialConfigurations = byobServiceIntegration.retrieveAll();

        if (credentialConfigurations == null || credentialConfigurations.getCredentialConfigurations() == null || credentialConfigurations.getCredentialConfigurations().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, DynamicCredentialConfiguration> ccMap = new HashMap<>();
        for (DynamicCredentialConfiguration cc : credentialConfigurations.getCredentialConfigurations()) {
            ccMap.put(cc.credentialConfigurationId(), cc);

        }
        log.info("Retrieved all credential-configuration from byob-service credential_configuration_ids: %s".formatted(ccMap.keySet()));
        return ccMap;
    }

    public List<CredentialConfigurationProperties> generateCredentialConfigurations() {
        Map<String, DynamicCredentialConfiguration> configs;
        try {
            configs = getDynamicCredentialConfigurations();
        } catch (RuntimeException e) {
            log.error("Failed to fetch dynamic credential configurations from BYOB service. Continue without BYO-bevis", e);
            return Collections.emptyList();
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
        if (credentialType == null) {
            log.info("credentialType is null, cannot retrieve credential-configuration from byob-service");
            return null;
        }
        DynamicCredentialConfiguration cc = byobServiceIntegration.retrieve(credentialType);
        if (cc != null) {
            log.info("Retrieved credential-configuration from byob-service by credentialType: %s".formatted(credentialType));
            return cc.getCredentialMetadata();
        }
//        return getDynamicCredentialConfigurations().values().stream().filter(dcc -> credentialType.equals(dcc.vct())).findFirst().map(DynamicCredentialConfiguration::getCredentialMetadata).orElse(null);
        return null;
    }

    public DocumentMetadata getDocumentMetadata(String credentialConfigurationId) {
        if (credentialConfigurationId == null) {
            log.warn("Failed to retrieved credential-configuration from byob-service by credentialConfigurationId: null");
            return null;
        }

        DynamicCredentialConfiguration cc = byobServiceIntegration.searchByCredentialConfigurationId(credentialConfigurationId);
        if (cc != null) {
            log.info("Retrieved credential-configuration from byob-service by credentialConfigurationId: %s".formatted(credentialConfigurationId));
            return cc.getCredentialMetadata();
        }
        return null;
        //return getDynamicCredentialConfigurations().values().stream().filter(dcc -> credentialConfigurationId.equals(dcc.credentialConfigurationId())).findFirst().map(DynamicCredentialConfiguration::getCredentialMetadata).orElse(null);
    }

}
