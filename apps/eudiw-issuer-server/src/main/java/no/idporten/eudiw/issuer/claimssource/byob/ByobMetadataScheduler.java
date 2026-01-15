package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.openid4vci.CredentialIssuerServerGeneratorService;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialConfiguration;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialIssuerMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ByobMetadataScheduler {

    private final CredentialIssuerMetadata credentialIssuerMetadata;
    private final CredentialIssuerServerGeneratorService credentialIssuerServerGeneratorService;

    @Autowired
    public ByobMetadataScheduler(CredentialIssuerMetadata credentialIssuerMetadata, CredentialIssuerServerGeneratorService credentialIssuerServerGeneratorService) {
        this.credentialIssuerMetadata = credentialIssuerMetadata;
        this.credentialIssuerServerGeneratorService = credentialIssuerServerGeneratorService;
    }

    @Scheduled(fixedDelayString = "${credential-issuer-server.metadata-refresh-rate-in-millis}", initialDelay = 60000)
    public void updateIssuerMetadata() {
        Map<String, CredentialConfiguration> ccs = credentialIssuerServerGeneratorService.getByobCredentialConfigurations();
        for (String credentialConfigurationId : ccs.keySet()) {
            CredentialConfiguration cc = ccs.get(credentialConfigurationId);
            credentialIssuerMetadata.addCredentialConfigurations(credentialConfigurationId, cc);
        }
    }
}
