package no.idporten.eudiw.login.openid4vp;

import no.digdir.oidc.redis.service.CustomObjectsCache;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Cache wallet interactions.
 */
@Component
public class WalletInteractionCache {

    private final CustomObjectsCache cache;
    private final String CACHE_PREFIX_OPENID4VP = ":openid4vp:";

    public WalletInteractionCache(CustomObjectsCache cache) {
        this.cache = cache;
    }

    public void putWalletInteraction(String id, WalletInteraction walletInteraction) {
        cache.put(CACHE_PREFIX_OPENID4VP + id, walletInteraction, Duration.ofSeconds(walletInteraction.expiresInSeconds()));
    }

    public WalletInteraction getWalletInteraction(String id) {
        return (WalletInteraction) cache.get(CACHE_PREFIX_OPENID4VP + id);
    }

    public void removeWalletInteraction(String id) {
        cache.remove(CACHE_PREFIX_OPENID4VP + id);
    }

}
