package no.idporten.eudiw.login.openid4vp;

import no.digdir.oidc.redis.service.RedisOpenIDConnectCache;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Cache string wallet interactions.
 */
@Component
public class WalletInteractionRedisCache {

    private final RedisOpenIDConnectCache redisDataSource;
    private final String redis_key = ":openid4vp:";

    public WalletInteractionRedisCache(RedisOpenIDConnectCache redisDataSource) {
        this.redisDataSource = redisDataSource;
    }

    public void putWalletInteraction(String id, WalletInteraction walletInteraction) {
        redisDataSource.put(redis_key + id, walletInteraction, Duration.ofSeconds(walletInteraction.expiresInSeconds()));
    }

    public WalletInteraction getWalletInteraction(String id) {
        return (WalletInteraction) redisDataSource.get(redis_key + id);
    }

    public void removeWalletInteraction(String id) {
        redisDataSource.remove(redis_key + id);
    }

}
