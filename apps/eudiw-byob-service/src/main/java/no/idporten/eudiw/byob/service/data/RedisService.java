package no.idporten.eudiw.byob.service.data;

import jakarta.annotation.PostConstruct;
import no.idporten.eudiw.byob.service.exception.BadRequestException;
import no.idporten.eudiw.byob.service.model.data.CredentialConfigurationData;
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
    public static final String DATA_PREFIX_BYOB_TYPES = APP_NAME + "credential-configuration-types:"; // Key-data map (credentialType -> credential-configuration)
    public static final String KEY_PREFIX_BYOB_ID = APP_NAME + "credential-types:"; // Key mapping (credential-configuration-id -> credentialType). Can use to lookup data (credential-configuration by credentialType) afterwards.

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
    }

    public void addBevisType(CredentialConfigurationData bevisType) {
        valueOperations.set(DATA_PREFIX_BYOB_TYPES + bevisType.credentialType(), bevisType);
        valueOperations.set(KEY_PREFIX_BYOB_ID + bevisType.credentialConfigurationId(), bevisType.credentialType());
        log.info("Added BevisType to Redis: credentialType={}", bevisType.credentialType());
    }

    public void updateBevisType(CredentialConfigurationData bevisType) {
        Object oldBevisType = valueOperations.getAndSet(DATA_PREFIX_BYOB_TYPES + bevisType.credentialType(), bevisType);
        Object oldCredentialType = valueOperations.getAndSet(KEY_PREFIX_BYOB_ID + bevisType.credentialConfigurationId(), bevisType.credentialType());
        if (oldBevisType == null || oldCredentialType == null) {
            log.error("Tried to update bevistype in Redis, but did not exist: credentialType={}, so is created instead. This should never happen.", bevisType.credentialType());
        }
        log.info("Updated BevisType to Redis: credentialType={}", bevisType.credentialType());
    }

    public CredentialConfigurationData getBevisType(String credentialType) {
        return (CredentialConfigurationData) valueOperations.get(DATA_PREFIX_BYOB_TYPES + credentialType);
    }

    public CredentialConfigurationData getBevisTypeByCredentialConfiguration(String credentialConfigurationId) {
        String credentialType = (String) valueOperations.get(KEY_PREFIX_BYOB_ID + credentialConfigurationId);
        return (CredentialConfigurationData) valueOperations.get(DATA_PREFIX_BYOB_TYPES + credentialType);
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

    public void delete(String credentialType) {
        CredentialConfigurationData bevisType = getBevisType(credentialType);
        if (bevisType == null) {
            String errorMsg = getErrorMessage(credentialType);
            log.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        String deletedCredentialConfigurationId = (String) valueOperations.getAndDelete(KEY_PREFIX_BYOB_ID + bevisType.credentialConfigurationId());
        CredentialConfigurationData data = (CredentialConfigurationData) valueOperations.getAndDelete(DATA_PREFIX_BYOB_TYPES + credentialType);
        if (deletedCredentialConfigurationId == null || data == null) {
            log.warn(getErrorMessage(credentialType) + " (credentialConfigurationId={}, data={})", deletedCredentialConfigurationId, data);
            throw new BadRequestException(getErrorMessage(credentialType));
        } else {
            log.info("Deleted BevisType from Redis: credentialType={}, credentialConfigurationId={}", data.credentialType(), deletedCredentialConfigurationId);
        }
    }

    private static String getErrorMessage(String credentialType) {
        return "Failed to delete BevisType from Redis for credentialType=%s since does not exist".formatted(credentialType);
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
            log.info("Deleted BevisType from Redis: credentialType={}, credentialConfigurationId={}", data.credentialType(), ccId);
        }
        log.info("Deleted all BevisType from Redis");
    }

    private Set<String> getAllDataKeys() {
        return redisTemplate.keys(DATA_PREFIX_BYOB_TYPES + "*");
    }

}
