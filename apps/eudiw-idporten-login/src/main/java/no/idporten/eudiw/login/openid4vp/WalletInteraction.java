package no.idporten.eudiw.login.openid4vp;

import no.idporten.sdk.oidcserver.cache.Cacheable;

import java.time.Instant;


public class WalletInteraction implements Cacheable {

    private long createdAtEpochMillis;
    private long expiresAtEpochMillis;
    private OpenID4VPFlow flow;
    private String id;
    private String personIdentifier;

    public OpenID4VPFlow getFlow() {
        return flow;
    }

    public String getId() {
        return id;
    }

    public void setPersonIdentifier(String personIdentifier) {
        this.personIdentifier = personIdentifier;
    }

    public String getPersonIdentifier() {
        return personIdentifier;
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

    public void setFlow(OpenID4VPFlow flow) {
        this.flow = flow;
    }

}
