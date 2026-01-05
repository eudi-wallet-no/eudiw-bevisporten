package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DynamicCredentialConfigurationService {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    private static Logger log = LoggerFactory.getLogger(DynamicCredentialConfigurationService.class);


    public static final String DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX = "net.eidas2sandkasse:";

    private static final DynamicCredentialConfiguration DYNAMIC_CREDENTIAL_CONFIGURATION_1 = DynamicCredentialConfiguration.builder()
            .credentialConfigurationId("net.eidas2sandkasse:dynamic:1_sd_jwt_vc")
            .vct("dynamic:1")
            .format(CredentialFormat.SD_JWT_VC.formatIdentifier())
            .credentialMetadata(new DocumentMetadata(
                            List.of(
                                    new DocumentMetadata.Display("no", "Bring ditt eget bevis"),
                                    new DocumentMetadata.Display("en", "Bring your own bevis")
                            ),
                            List.of(
                                    new ClaimMetadata("name",
                                            Map.of(
                                                    "no", "Navn",
                                                    "en", "Name"),
                                            true,
                                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$")
                            )
                    )
            )
            .build();

    private static final DynamicCredentialConfiguration DYNAMIC_CREDENTIAL_CONFIGURATION_2 = DynamicCredentialConfiguration.builder()
            .credentialConfigurationId("net.eidas2sandkasse:dynamic:2_sd_jwt_vc")
            .vct("dynamic:2")
            .format(CredentialFormat.SD_JWT_VC.formatIdentifier())
            .credentialMetadata(new DocumentMetadata(
                            List.of(
                                    new DocumentMetadata.Display("no", "bevis 2")
                            ),
                            List.of(
                                    new ClaimMetadata("age",
                                            Map.of(
                                                    "no", "Alder"),
                                            true,
                                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$")
                            )
                    )
            )
            .build();



    private Map<String, DynamicCredentialConfiguration> dynamicCredentialConfigurations =
            Map.of(
                    DYNAMIC_CREDENTIAL_CONFIGURATION_1.getCredentialConfigurationId(), DYNAMIC_CREDENTIAL_CONFIGURATION_1,
                    DYNAMIC_CREDENTIAL_CONFIGURATION_2.getCredentialConfigurationId(), DYNAMIC_CREDENTIAL_CONFIGURATION_2);

    public DynamicCredentialConfigurationService(CredentialIssuerServerProperties credentialIssuerServerProperties) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
    }


    public List<CredentialConfigurationProperties> generateCredentialConfigurations() {
        return dynamicCredentialConfigurations.keySet().stream().map(this::generateCredentialConfiguration).toList();
    }

    public CredentialConfigurationProperties generateCredentialConfiguration(String credentialConfigurationId) {
        CredentialConfigurationProperties dynamicCredentialConfigurationTemplate = credentialIssuerServerProperties.getDynamicCredentialConfigurationTemplate();
        DynamicCredentialConfiguration dynamicCredentialConfiguration = dynamicCredentialConfigurations.get(credentialConfigurationId);
        CredentialConfigurationProperties credentialConfiguration = new CredentialConfigurationProperties();
        credentialConfiguration.setIdentifier(dynamicCredentialConfiguration.getCredentialConfigurationId());
        credentialConfiguration.setCredentialType(dynamicCredentialConfiguration.getVct());
        credentialConfiguration.setFormat(CredentialFormat.SD_JWT_VC);
        credentialConfiguration.setValidityDays(30);
        credentialConfiguration.setScope(dynamicCredentialConfigurationTemplate.getScope());
        credentialConfiguration.setGrantType(dynamicCredentialConfigurationTemplate.getGrantType());
        credentialConfiguration.setAuthorizationServer(dynamicCredentialConfigurationTemplate.getAuthorizationServer());
        credentialConfiguration.setPreAuthorizationServer(dynamicCredentialConfigurationTemplate.getPreAuthorizationServer());
        credentialConfiguration.setKeyStoreName(dynamicCredentialConfigurationTemplate.getKeyStoreName());
        return credentialConfiguration;
    }

    public DocumentMetadata getDocumentMetadataByCredentialType(String credentialType) {
        return dynamicCredentialConfigurations.values().stream().filter(dcc -> credentialType.equals(dcc.getVct())).findFirst().map(DynamicCredentialConfiguration::getCredentialMetadata).orElse(null);
    }

    public DocumentMetadata getDocumentMetadata(String credentialConfigurationId) {
        return dynamicCredentialConfigurations.values().stream().filter(dcc -> credentialConfigurationId.equals(dcc.getCredentialConfigurationId())).findFirst().map(DynamicCredentialConfiguration::getCredentialMetadata).orElse(null);
    }


//    public DynamicCredentialConfiguration getDocumentMetadata(String credentialType) {
//        log.info("Getting dynamic credentials for credential type {}", credentialType);
//        return dynamicCredentialConfigurations.values().stream().filter(dcc -> credentialType.equals(dcc.getVct())).findFirst().map(DynamicCredentialConfiguration::getCredentialMetadata).orElse(null);
//    }


}
