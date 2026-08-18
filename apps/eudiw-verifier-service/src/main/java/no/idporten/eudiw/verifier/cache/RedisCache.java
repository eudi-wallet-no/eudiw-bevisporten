package no.idporten.eudiw.verifier.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RedisCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(RedisCache.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.cache.redis.time-to-live}")
    private Duration expire;

    public RedisCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void set(String cacheKey, Object object) {
        try {
            redisTemplate.opsForValue().set(cacheKey, object, expire);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to set {} object in cache: {}", cacheKey, e.getMessage());
            throw e;
        }
    }

    @Override
    public Object get(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to get {} object from cache: {}", cacheKey, e.getMessage());
            throw e;
        }
    }

    @Override
    public Object remove(String cacheKey) {
        try {
            return redisTemplate.opsForValue().getAndDelete(cacheKey);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to delete {} object from cache: {}", cacheKey, e.getMessage());
            throw e;
        }
    }
}