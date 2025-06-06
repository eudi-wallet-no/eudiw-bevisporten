package no.idporten.sdk.oidcserver.cache;

import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When checking cacheable object validity")
public class CacheableTest {

    static class CachedObject implements Cacheable {
        private long createdAtEpochMillis;
        private long expiresAtEpochMillis;
        @Setter
        private long timeSKewMillis;

        @Override
        public long createdAtEpochMillis() {
            return createdAtEpochMillis;
        }

        @Override
        public long expiresAtEpochMillis() {
            return expiresAtEpochMillis;
        }

        @Override
        public long timeSkewMillis() {
            return timeSKewMillis;
        }

        @Override
        public void setLifetimeSeconds(long lifetimeSeconds) {
            this.createdAtEpochMillis = Instant.now().toEpochMilli();
            this.expiresAtEpochMillis = createdAtEpochMillis + (lifetimeSeconds * 1000);
        }

    }

    @DisplayName("then the object will expire when it's lifetime is over")
    @Test
    void testExpiresIn() throws InterruptedException {
        CachedObject cachedObject = new CachedObject();
        cachedObject.setLifetimeSeconds(1);
        assertAll(
                () -> assertTrue(cachedObject.isValidNow()),
                () -> assertTrue(cachedObject.expiresInSeconds() <= 1)
        );
        sleep(1100);
        assertAll(
                () -> assertFalse(cachedObject.isValidNow()),
                () -> assertTrue(cachedObject.expiresInSeconds() <= 0)
        );
    }

    @DisplayName("then a time skew can be applied to relax the check")
    @Test
    void testTimeSkew() {
        CachedObject cachedObject = new CachedObject();
        cachedObject.setLifetimeSeconds(1);
        cachedObject.setTimeSKewMillis(1000);
        assertAll(
                () -> assertTrue(cachedObject.isValidNow()),
                () -> assertTrue(cachedObject.isValidAt(Instant.now().toEpochMilli() - 500)),
                () -> assertTrue(cachedObject.isValidAt(Instant.now().toEpochMilli() + 1500)),
                () -> assertTrue(cachedObject.expiresInSeconds() <= 1)
        );
    }

}
