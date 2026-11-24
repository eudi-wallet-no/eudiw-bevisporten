package net.eidas2sandkasse.trustlist;

import java.net.URI;
import java.security.cert.X509Certificate;

class TrustlistValidatorTest {

    @Test
    @DisplayName("When validate is called with valid input, true is expected")
    void positiveTestTrustlistCheck() {
        TrustlistValidator validator = new TrustlistValidator();
        URI uri = URI.create("https://example.com/trustlist");
        X509Certificate certificate = mock(X509Certificate.class);
        boolean result = validator.validate(uri, certificate);
        assertTrue(result);
    }
}