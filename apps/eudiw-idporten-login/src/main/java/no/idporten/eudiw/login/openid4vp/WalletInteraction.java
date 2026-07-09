package no.idporten.eudiw.login.openid4vp;

import no.idporten.sdk.oidcserver.cache.Cacheable;

import java.time.Instant;

public class WalletInteraction implements Cacheable {

    private long createdAtEpochMillis;
    private long expiresAtEpochMillis;
    private final String id;
    private String verifierTransactionId;
    private boolean started;

    public String getId() {
        return id;
    }

    public WalletInteraction(String id) {
        setLifetimeSeconds(60 * 10); // 10 minutes
        this.id = id;
    }

    @Override
    public void setLifetimeSeconds(long lifetimeSeconds) {
        this.createdAtEpochMillis = Instant.now().toEpochMilli();
        this.expiresAtEpochMillis = createdAtEpochMillis + (lifetimeSeconds * 1000);
    }

    @Override
    public long createdAtEpochMillis() {
        return createdAtEpochMillis;
    }

    @Override
    public long expiresAtEpochMillis() {
        return expiresAtEpochMillis;
    }

    public String getVerifierTransactionId() {
        return verifierTransactionId;
    }

    public void setVerifierTransactionId(String verifierTransactionId) {
        this.verifierTransactionId = verifierTransactionId;
        this.started = true;
    }

    public boolean isStarted() {
        return started;
    }

}
