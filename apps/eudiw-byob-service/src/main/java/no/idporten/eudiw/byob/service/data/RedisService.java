package no.idporten.eudiw.byob.service.data;

import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.byob.service.exception.BadRequestException;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.data.CredentialConfigurationData;
import no.idporten.eudiw.byob.service.service.MockCredentialConfigurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RedisService {

    private final Logger log = LoggerFactory.getLogger(RedisService.class);

    public static final String APP_NAME = "byob-service:";
    public static final String DATA_PREFIX_BYOB_TYPES = APP_NAME + "credential-configuration-types:"; // Key-data map (vct -> credential-configuration)
    public static final String KEY_PREFIX_BYOB_ID = APP_NAME + "credential-types:"; // Key mapping (credential-configuration-id -> vct). Can use to lookup data (credential-configuration by vct) afterwards.

    private final RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOperations;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    private void init() {
        valueOperations = redisTemplate.opsForValue();
        Set<String> allKeys = getAllDataKeys();
        log.info("RedisService initialized. Found existing keys with prefix {} in Redis: {}", DATA_PREFIX_BYOB_TYPES, allKeys);
        addMockedCredentialConfigurationsToRedis();
    }

    // remove me when mocked data is no longer needed
    private void addMockedCredentialConfigurationsToRedis() {
        List<CredentialConfiguration> credentialConfigurationsMocked = MockCredentialConfigurations.getCredentialConfigurationsListMocked();
        for (CredentialConfiguration credentialConfiguration : credentialConfigurationsMocked) {
            CredentialConfigurationData bevisTypeData = new CredentialConfigurationData(credentialConfiguration);
            addBevisType(bevisTypeData);
            log.info("Added mocked BevisType to Redis: vct={}", credentialConfiguration.vct()); // will override or not add if already exists
        }
        log.info("RedisService initialization complete. Total BevisTypes in Redis: {}", getAll().size());
    }

    public void addBevisType(CredentialConfigurationData bevisType) {
        valueOperations.set(DATA_PREFIX_BYOB_TYPES + bevisType.vct(), bevisType);
        valueOperations.set(KEY_PREFIX_BYOB_ID + bevisType.credentialConfigurationId(), bevisType.vct());
        log.info("Added BevisType to Redis: vct={}", bevisType.vct());
    }

    public CredentialConfigurationData getBevisType(String vct) {
        return (CredentialConfigurationData) valueOperations.get(DATA_PREFIX_BYOB_TYPES + vct);
    }

    public CredentialConfigurationData getBevisTypeByCredentialConfiguration(String credentialConfigurationId) {
        String vct = (String) valueOperations.get(KEY_PREFIX_BYOB_ID + credentialConfigurationId);
        return (CredentialConfigurationData) valueOperations.get(DATA_PREFIX_BYOB_TYPES + vct);
    }

    // TODO: Performance improvement: possible to fetch all values without fetching each individually by key?
    public List<CredentialConfigurationData> getAll() {
        Set<String> keys = getAllDataKeys();
        List<CredentialConfigurationData> all = new ArrayList<>();
        for (String key : keys) {
            valueOperations.get(key);
            all.add((CredentialConfigurationData) valueOperations.get(key));
        }
        return all;
    }

    public void delete(String vct) {
        CredentialConfigurationData bevisType = getBevisType(vct);
        if (bevisType == null) {
            String errorMsg = getErrorMessage(vct);
            log.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        String deletedCredentialConfigurationId = (String) valueOperations.getAndDelete(KEY_PREFIX_BYOB_ID + bevisType.credentialConfigurationId());
        CredentialConfigurationData data = (CredentialConfigurationData) valueOperations.getAndDelete(DATA_PREFIX_BYOB_TYPES + vct);
        if (deletedCredentialConfigurationId == null || data == null) {
            log.warn(getErrorMessage(vct) + " (credentialConfigurationId={}, data={})", deletedCredentialConfigurationId, data);
            throw new BadRequestException(getErrorMessage(vct));
        } else {
            log.info("Deleted BevisType from Redis: vct={}, credentialConfigurationId={}", data.vct(), deletedCredentialConfigurationId);
        }
    }

    private static String getErrorMessage(String vct) {
        String errorMsg = "Failed to delete BevisType from Redis for vct=%s since does not exist".formatted(vct);
        return errorMsg;
    }


    public void deleteAll() {
        Set<String> keys = getAllDataKeys();
        for (String key : keys) {
            CredentialConfigurationData data = (CredentialConfigurationData) valueOperations.getAndDelete(key);
            if (data == null) {
                log.error("Failed to delete BevisType from Redis for key={}", key);
                return;
            }
            String ccId = (String) valueOperations.getAndDelete(DATA_PREFIX_BYOB_TYPES + data.credentialConfigurationId());
            log.info("Deleted BevisType from Redis: vct={}, credentialConfigurationId={}", data.vct(), ccId);
        }
        log.info("Deleted all BevisType from Redis");
    }

    private Set<String> getAllDataKeys() {
        return redisTemplate.keys(DATA_PREFIX_BYOB_TYPES + "*");
    }

}
