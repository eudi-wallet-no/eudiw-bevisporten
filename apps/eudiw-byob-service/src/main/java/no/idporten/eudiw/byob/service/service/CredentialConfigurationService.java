package no.idporten.eudiw.byob.service.service;

import no.idporten.eudiw.byob.service.data.RedisService;
import no.idporten.eudiw.byob.service.exception.BadRequestException;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.CredentialConfigurations;
import no.idporten.eudiw.byob.service.model.CredentialMetadata;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;
import no.idporten.eudiw.byob.service.model.data.CredentialConfigurationData;
import no.idporten.eudiw.byob.service.model.web.CredentialConfigurationRequestResource;
import no.idporten.eudiw.byob.service.model.web.ExampleCredentialDataRequestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CredentialConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(CredentialConfigurationService.class);

    public static final String VCT_PREFIX = "net.eidas2sandkasse:";
    private static final String SD_JWT_VC = "_sd_jwt_vc";

    private final RedisService redisService;

    @Autowired
    public CredentialConfigurationService(RedisService redisService) {
        this.redisService = redisService;
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
        if (redisService.getBevisType(vct) != null) {
            throw new BadRequestException("Credential configuration already exists for vct=%s".formatted(credentialConfiguration.vct()));
        }
        String credentialConfigurationId = VCT_PREFIX + credentialConfiguration.vct() + SD_JWT_VC;
        CredentialConfiguration cc = convert(credentialConfiguration, credentialConfigurationId, vct);
        redisService.addBevisType(new CredentialConfigurationData(cc));
        log.info("Generated new credential configuration for vct: {}", cc.vct());
        return cc;
    }

    private static CredentialConfiguration convert(CredentialConfigurationRequestResource credentialConfiguration, String credentialConfigurationId, String vct) {
        List<ExampleCredentialData> exampleCredentialData = convertExampleData(credentialConfiguration.exampleCredentialData());
        CredentialMetadata credentialMetadata = credentialConfiguration.credentialMetadata().toCredentialMetadata();
        return new CredentialConfiguration(
                credentialConfigurationId,
                vct,
                credentialConfiguration.format(),
                exampleCredentialData,
                credentialMetadata
        );
    }

    private static List<ExampleCredentialData> convertExampleData(List<ExampleCredentialDataRequestResource> exampleCredentialData) {
        if(exampleCredentialData == null || exampleCredentialData.isEmpty()) {
            return Collections.emptyList();
        }
        return exampleCredentialData.stream().map(ExampleCredentialDataRequestResource::toExampleCredentialData).toList();
    }

    public CredentialConfigurations getAllEntries() {
        List<CredentialConfigurationData> all = redisService.getAll();
        List<CredentialConfiguration> list = all.stream().map(CredentialConfigurationData::toCredentialConfiguration).toList();
        return new CredentialConfigurations(list);
    }

    public CredentialConfiguration getCredentialConfiguration(String vct) {
        CredentialConfigurationData data = redisService.getBevisType(vct);
        if (data == null) { return null;}
        return data.toCredentialConfiguration();
    }

    public CredentialConfiguration searchCredentialConfiguration(String credentialConfigurationId) {
        CredentialConfigurationData data = redisService.getBevisTypeByCredentialConfiguration(credentialConfigurationId);
        if (data == null) { return null;}
        return data.toCredentialConfiguration();
    }
}
