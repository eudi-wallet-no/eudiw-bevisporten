package no.idporten.eudiw.oauth2.server.protocol;

import no.idporten.eudiw.oauth2.server.cache.Cacheable;

import java.io.Serial;
import java.time.Instant;
import java.util.*;

public class Challenge implements Cacheable, AuditDataProvider {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String challenge;
    private long createdAtEpochMillis;
    private long expiresAtEpochMillis;

    public Challenge(String challenge, long lifetimeSeconds) {
        this.challenge = challenge;
        setLifetimeSeconds(lifetimeSeconds);
    }

    public String challenge() {
        return challenge;
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

    @Override
    public AuditData getAuditData() {
        Map<String, Object> auditData = new HashMap<>();
        return AuditData.builder()
                .attribute("challenge", challenge)
                .build();
    }

}
