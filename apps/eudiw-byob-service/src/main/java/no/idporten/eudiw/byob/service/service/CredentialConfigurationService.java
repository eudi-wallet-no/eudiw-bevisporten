package no.idporten.eudiw.byob.service.service;

import no.idporten.eudiw.byob.service.exception.BadRequestException;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.CredentialMetadata;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;
import no.idporten.eudiw.byob.service.model.web.CredentialConfigurationRequestResource;
import no.idporten.eudiw.byob.service.model.CredentialConfigurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CredentialConfigurationService {

    private final Map<String, CredentialConfiguration> persistenceLayer = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(CredentialConfigurationService.class);
    private int counter = 0;
    public static final String VCT_PREFIX = "net.eidas2sandkasse:";
    private static final String SD_JWT_VC = "_sd_jwt_vc";

    public CredentialConfigurationService() {
        persistenceLayer.putAll(MockCredentialConfigurations.getCredentialConfigurationsMocked());
    }

    /**
     * Takes in user input, sends input to check if the VCT is already registered, and if
     * not already registered, it gives back a generated id which is not used for anything
     * as well as the credentialConfiguration.
     *
     * @param credentialConfiguration user input that user POSTS in to BYOB in order to "build your own bevis"
     * @return a new CredentialConfiguration with an id that consists of a set prefix, the VCT given in input plus format.
     */
    public CredentialConfiguration create(CredentialConfigurationRequestResource credentialConfiguration) {
        String vct = credentialConfiguration.vct();
        if (!credentialConfiguration.vct().startsWith(VCT_PREFIX)) {
            vct = VCT_PREFIX + vct;
        }
        if (persistenceLayer.containsKey(vct)) {
            throw new BadRequestException("Credential configuration already exists for vct=%s".formatted(credentialConfiguration.vct()));
        }
        String credentialConfigurationId = VCT_PREFIX + credentialConfiguration.vct() + SD_JWT_VC;
        CredentialConfiguration cc = convert(credentialConfiguration, credentialConfigurationId, vct);
        updatePersistenceLayer(cc);
        log.info("Generated new credential configuration for vct: {}", cc.vct());
        return cc;
    }

    private static CredentialConfiguration convert(CredentialConfigurationRequestResource credentialConfiguration, String credentialConfigurationId, String vct) {
        ExampleCredentialData exampleCredentialData = credentialConfiguration.exampleCredentialData().toExampleCredentialData();
        CredentialMetadata credentialMetadata = credentialConfiguration.credentialMetadata().toCredentialMetadata();
        return new CredentialConfiguration(
                credentialConfigurationId,
                vct,
                credentialConfiguration.format(),
                exampleCredentialData,
                credentialMetadata
        );
    }

    public void updatePersistenceLayer(CredentialConfiguration credentialConfiguration) {
        this.persistenceLayer.put(credentialConfiguration.vct(), credentialConfiguration);
    }

    public CredentialConfigurations getAllEntries() {
        return new CredentialConfigurations(persistenceLayer.values().stream().toList());

    }

    public CredentialConfiguration getCredentialConfiguration(String vct) {
        return persistenceLayer.get(vct);
    }

    public CredentialConfiguration searchCredentialConfiguration(String credentialConfigurationId) {
        return persistenceLayer.values().stream().filter(c -> c.credentialConfigurationId().equals(credentialConfigurationId)).findFirst().orElse(null);
    }
}
