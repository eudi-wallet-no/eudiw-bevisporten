package no.idporten.eudiw.login.openid4vp;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service tracking wallet interactions.  Used for polling.
 */
@Service
public class WalletInteractionService {

    private final WalletInteractionCache walletInteractionCache;

    public WalletInteractionService(WalletInteractionCache walletInteractionCache) {
        this.walletInteractionCache = walletInteractionCache;
    }

    protected String createWalletInteractionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Starts a new wallet interaction.  Generates id.
     */
    public WalletInteraction createWalletInteraction() {
        WalletInteraction walletInteraction = new WalletInteraction(createWalletInteractionId());
        walletInteractionCache.putWalletInteraction(walletInteraction.getId(), walletInteraction);
        return walletInteraction;
    }

    public WalletInteraction updateWalletInteraction(WalletInteraction walletInteraction) {
        walletInteractionCache.putWalletInteraction(walletInteraction.getId(), walletInteraction);
        return walletInteraction;
    }

    public WalletInteraction retrieveWalletInteraction(String id) {
        return walletInteractionCache.getWalletInteraction(id);
    }

    public void removeWalletInteraction(String state) {
        walletInteractionCache.removeWalletInteraction(state);
    }
}
