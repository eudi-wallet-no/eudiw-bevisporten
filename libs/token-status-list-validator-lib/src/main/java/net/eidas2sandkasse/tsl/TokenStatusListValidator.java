package net.eidas2sandkasse.tsl;

import io.ktor.client.engine.HttpClientEngine;
import net.eidas2sandkasse.tsl.walt.WaltStatusListClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Clock;

/**
 * Validates a credential against an IETF Token Status List, as statuslist+jwt or statuslist+cwt.
 * The result includes the certificate the list was signed with.
 */
public class TokenStatusListValidator {

    private static final Logger log = LoggerFactory.getLogger(TokenStatusListValidator.class);

    private final WaltStatusListClient client;

    public TokenStatusListValidator() {
        this(TokenStatusListValidatorConfig.defaults());
    }

    public TokenStatusListValidator(TokenStatusListValidatorConfig config) {
        this.client = new WaltStatusListClient(
                config.connectTimeout().toMillis(),
                config.readTimeout().toMillis(),
                config.cacheEnabled(),
                config.clockSkew().toSeconds());
    }

    // For tests
    TokenStatusListValidator(TokenStatusListValidatorConfig config, Clock clock, HttpClientEngine engine) {
        this.client = new WaltStatusListClient(
                config.connectTimeout().toMillis(),
                config.readTimeout().toMillis(),
                config.cacheEnabled(),
                config.clockSkew().toSeconds(),
                clock,
                engine);
    }

    /**
     * Resolves the status at index in a verified token status list. Check the signer certificate against a trust list
     * before trusting the status.
     *
     * @throws TokenStatusListException if the status could not be determined, or the list is not signed with an
     *                                  x5c/x5chain certificate
     */
    public TokenStatus resolve(URI uri, long index) {
        if (!validUri(uri)  || !validIdx(index)) {
            throw new IllegalArgumentException("Status list uri and a non-negative index are required");
        }
        try {
            return client.resolveStatus(uri.toString(), (int) index);
        } catch (Exception e) {
            log.warn("Status list {} could not be resolved for idx {}", uri, index);
            throw new TokenStatusListException("Could not resolve token status list status", e);
        }
    }

    public boolean validIdx(long index) {
        if (index < 0 || index > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Status list index is invalid");
        }
        return true;
    }

    public boolean validUri(URI uri) {
        if (uri == null ) {
            throw new IllegalArgumentException("Status list uri is null");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Status list uri must be https");
        }
       return true;
    }
}
