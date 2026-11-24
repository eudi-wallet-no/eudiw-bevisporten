package net.eidas2sandkasse.trustlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.security.cert.X509Certificate;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustlistValidatorTest {
    private final X509Certificate certificate = null;

    @Test
    @DisplayName("When validate is called with valid input, true is expected")
    void positiveTestTrustlistCheck() {
        TrustlistValidator validator = new TrustlistValidator();
        URI uri = URI.create("https://example.com/trustlist");
        boolean result = validator.validate(uri, certificate);
        assertTrue(result);
    }
}