package net.eidas2sandkasse.tsl;

import java.time.Duration;

/**
 * @param connectTimeout TCP connect timeout against status list endpoints
 * @param readTimeout read timeout for status list endpoints
 * @param cacheEnabled   cache status lists in memory (walt.id: max 256 lists, expiry from Cache-Control, exp and ttl)
 * @param clockSkew      allowed clock difference when timestamps
 */
public record TokenStatusListConfig(Duration connectTimeout, Duration readTimeout, boolean cacheEnabled,
                                    Duration clockSkew) {

    public static TokenStatusListConfig defaults() {
        return new TokenStatusListConfig(Duration.ofSeconds(3), Duration.ofSeconds(5), true, Duration.ofSeconds(60));
    }
}
