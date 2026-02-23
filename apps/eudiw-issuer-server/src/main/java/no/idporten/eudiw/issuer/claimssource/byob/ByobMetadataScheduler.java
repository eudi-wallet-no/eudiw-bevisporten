package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.config.CredentialConfigurationService;
import no.idporten.eudiw.issuer.openid4vci.CredentialIssuerServerGeneratorService;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialConfiguration;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialIssuerMetadata;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Component
public class ByobMetadataScheduler {

   private final Logger log = LoggerFactory.getLogger(ByobMetadataScheduler.class);

    private final CredentialIssuerMetadata credentialIssuerMetadata;
    private final CredentialIssuerServerGeneratorService credentialIssuerServerGeneratorService;
    private final CredentialConfigurationService credentialConfigurationService;

    @Autowired
    public ByobMetadataScheduler(CredentialIssuerMetadata credentialIssuerMetadata, CredentialIssuerServerGeneratorService credentialIssuerServerGeneratorService, CredentialConfigurationService credentialConfigurationService) {
        this.credentialIssuerMetadata = credentialIssuerMetadata;
        this.credentialIssuerServerGeneratorService = credentialIssuerServerGeneratorService;
        this.credentialConfigurationService = credentialConfigurationService;
    }

    @Scheduled(fixedDelayString = "${credential-issuer-server.metadata-refresh-rate-in-millis}", initialDelayString = "${credential-issuer-server.metadata-refresh-rate-in-millis}")
    public void updateIssuerMetadata() {
        Map<String, CredentialConfiguration> ccs = credentialIssuerServerGeneratorService.getByobCredentialConfigurations();
        removeByobCredentialConfigurations(ccs);

        for (String credentialConfigurationId : ccs.keySet()) {
            CredentialConfiguration cc = ccs.get(credentialConfigurationId);
            credentialIssuerMetadata.addCredentialConfigurations(credentialConfigurationId, cc);
        }
        log.info("Updated credential-issuer-metadata added/updated {}", ccs.keySet());
    }

    private void removeByobCredentialConfigurations(Map<String, CredentialConfiguration> newByobCcs) {
        if (credentialIssuerMetadata == null || credentialIssuerMetadata.getCredentialConfigurations() == null || credentialIssuerMetadata.getCredentialConfigurations().isEmpty()) {
            return;
        }
        if (newByobCcs == null || newByobCcs.isEmpty()) {
            return;
        }
        List<String> keysToRemove = getDeletedCredentialConfigurationIds(newByobCcs, credentialIssuerMetadata.getCredentialConfigurations().keySet());
        for (String key : keysToRemove) {
            credentialIssuerMetadata.getCredentialConfigurations().remove(key);
            log.info("Removing BYOB credential configuration no longer present in BYOB service removed: {}", key);
        }

    }

    @NotNull
    private List<String> getDeletedCredentialConfigurationIds(Map<String, CredentialConfiguration> newByobCcs, Set<String> existingCredentialConfigurationIds) {
        List<String> keysToRemove = new ArrayList<>();
        for (String existingCC : existingCredentialConfigurationIds) {
            if (credentialConfigurationService.isStaticCredentialConfiguration(existingCC)) {
                continue;
            }
            if (newByobCcs.containsKey(existingCC)) {
                continue;
            }
            keysToRemove.add(existingCC);

        }
        return keysToRemove;
    }
}
