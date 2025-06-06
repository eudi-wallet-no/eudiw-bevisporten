package no.idporten.sdk.oidcserver.cache;

import java.io.Serializable;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining objects with a lifetime and that can put into a cache.
 */
public interface Cacheable extends Serializable {

    default boolean isValidNow() {
        return isValidAt(Instant.now().toEpochMilli());
    }

    default boolean isValidAt(long timestamp) {
        return createdAtEpochMillis() - timeSkewMillis() <= timestamp && timestamp < expiresAtEpochMillis() + timeSkewMillis();
    }

    default long timeSkewMillis() {
        return 1000;
    }

    default long expiresInSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(expiresInMillis());
    }

    default long expiresInMillis() {
        return expiresAtEpochMillis() - Instant.now().toEpochMilli();
    }

    long createdAtEpochMillis();
    long expiresAtEpochMillis();
    void setLifetimeSeconds(long lifetimeSeconds);

}
