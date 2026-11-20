package net.eidas2sandkasse.tsl.walt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The real status lists all have iat, so a missing iat is only covered here.
@DisplayName("When checking the times of a status list token")
class WaltStatusListClientTest {

    private static final String URI = "https://status.junit.eidas2sandkasse.dev/lists/1";
    private static final long NOW = 1_795_000_000L;
    private static final long SKEW = 60;

    @Test
    @DisplayName("and iat is missing or unreadable then an exception is expected")
    void missingIat() {
        assertThrows(IllegalArgumentException.class,
                () -> WaltStatusListClient.checkTokenTimes(URI, null, null, NOW, SKEW));
    }

    @Test
    @DisplayName("and iat is in the past and exp is missing then no exception is expected")
    void iatWithoutExp() {
        assertDoesNotThrow(() -> WaltStatusListClient.checkTokenTimes(URI, NOW - 1, null, NOW, SKEW));
    }

    @Test
    @DisplayName("and iat is exactly at the clock skew limit then no exception is expected")
    void iatAtSkewLimit() {
        assertDoesNotThrow(() -> WaltStatusListClient.checkTokenTimes(URI, NOW + SKEW, null, NOW, SKEW));
    }

    @Test
    @DisplayName("and exp is exactly at the clock skew limit then an exception is expected")
    void expAtSkewLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> WaltStatusListClient.checkTokenTimes(URI, NOW - 1, NOW - SKEW, NOW, SKEW));
    }
}
