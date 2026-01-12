package no.idporten.eudiw.byob.service.service;

import no.idporten.eudiw.byob.service.exception.BadRequestException;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
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
    public CredentialConfiguration getResponseModel(CredentialConfiguration credentialConfiguration) {
        return saveCredentialConfiguration(credentialConfiguration);
    }

    protected CredentialConfiguration saveCredentialConfiguration(CredentialConfiguration credentialConfiguration) {
        String vct = VCT_PREFIX + credentialConfiguration.vct();
        if (persistenceLayer.containsKey(vct)) {
            throw new BadRequestException("Credential configuration already exists for vct=%s".formatted(credentialConfiguration.vct()));
        }
        String credentialConfigurationId = VCT_PREFIX + credentialConfiguration.vct() + SD_JWT_VC;
        CredentialConfiguration cc = new CredentialConfiguration(
                credentialConfigurationId,
                vct,
                credentialConfiguration.format(),
                credentialConfiguration.exampleCredentialData(),
                credentialConfiguration.credentialMetadata()
        );
        updatePersistenceLayer(cc);
        log.info("Generated new credential configuration with id: {}", credentialConfiguration);
        return cc;
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
