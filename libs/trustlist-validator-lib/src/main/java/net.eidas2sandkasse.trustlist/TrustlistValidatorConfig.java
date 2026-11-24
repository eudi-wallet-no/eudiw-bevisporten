package net.eidas2sandkasse.trustlist;

import java.time.Duration;

/**
 * @param connectTimeout TCP connect timeout against trustlist endpoints
 * @param readTimeout read timeout for trustlist endpoints
 * @param cacheEnabled   cache status lists in memory (walt.id: max 256 lists, expiry from Cache-Control, exp and ttl)
 * @param clockSkew      allowed clock difference when timestamps
 */
public record TrustlistValidatorConfig(Duration connectTimeout,
                                             Duration readTimeout,
                                             boolean cacheEnabled,
                                             Duration clockSkew) {

    public static net.eidas2sandkasse.trustlist.TrustlistValidatorConfig defaults() {
        return new net.eidas2sandkasse.trustlist.TrustlistValidatorConfig(
                Duration.ofSeconds(3),
                Duration.ofSeconds(5),
                true,
                Duration.ofSeconds(60));
    }

}
