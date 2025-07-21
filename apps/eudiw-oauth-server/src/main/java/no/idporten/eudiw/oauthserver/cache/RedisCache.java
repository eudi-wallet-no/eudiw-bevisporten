package no.idporten.eudiw.oauthserver.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    public void put(String cacheKey, Object object, Duration duration) {
        try {
            redisTemplate.opsForValue().set(cacheKey, object, duration);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to set {} object in cache: {}", cacheKey, e.getMessage());
            throw e;
        }
    }

    public Object get(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to get {} object from cache: {}", cacheKey, e.getMessage());
            throw e;
        }
    }

    public Object remove(String cacheKey) {
        try {
            return redisTemplate.opsForValue().getAndDelete(cacheKey);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to delete {} object from cache: {}", cacheKey, e.getMessage());
            throw e;
        }
    }
}
