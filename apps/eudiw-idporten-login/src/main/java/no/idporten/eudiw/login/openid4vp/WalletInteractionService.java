package no.idporten.eudiw.login.openid4vp;

import org.springframework.stereotype.Service;

/**
 * Service tracking wallet interactions.  Stores interaction progress in cache.
 */
@Service
public class WalletInteractionService {

    private final WalletInteractionRedisCache openID4VPCache;

    public WalletInteractionService(WalletInteractionRedisCache openID4VPCache) {
        this.openID4VPCache = openID4VPCache;
    }

    public WalletInteraction startWalletInteraction(String id) {
        WalletInteraction walletInteraction = new WalletInteraction(id);
        openID4VPCache.putWalletInteraction(id, walletInteraction);
        return walletInteraction;
    }

    public WalletInteraction updateWalletInteraction(WalletInteraction walletInteraction) {
        openID4VPCache.putWalletInteraction(walletInteraction.getId(), walletInteraction);
        return walletInteraction;
    }

    public WalletInteraction getWalletInteraction(String id) {
        return openID4VPCache.getWalletInteraction(id);
    }

    public void removeWalletInteraction(String state) {
        openID4VPCache.removeWalletInteraction(state);
    }
}
