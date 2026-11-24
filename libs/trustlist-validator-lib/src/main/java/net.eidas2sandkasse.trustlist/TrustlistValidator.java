package net.eidas2sandkasse.trustlist;


import java.net.URI;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;


/**
 * Validates a certificate against a trustlist. It is valid if the certificate is on the trustlist or
 * chains to one on the trustlist.
 */

public class TrustlistValidator {


    private final HttpClient client;

    public TrustlistValidator() {
        this(TrustlistValidatorConfig.defaults());
    }

    public TrustlistValidator(TrustlistValidatorConfig config) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(config.connectTimeout().toMillis()))
                .build();
    }

    /**
     * Validates that the certificate is on a given trustlist or chains to one on that trustlist.
     *
     * @throws TrustlistException if something could not be determined (parsing error, network error, etc.)
     */
    public boolean validate(URI uri, X509Certificate certificate) {
        return checkTrustlist(uri, certificate);
    }

    /**
     * Checks the trustlist for the certificate or its chain.
     *
     * @return true if the certificate is on the trustlist or chains to one on the trustlist, false otherwise
     */
    private boolean checkTrustlist(URI uri, X509Certificate certificate) {
        validUri(uri);
        try {
//            TODO: Implementation logic. Use Walt id library to check the trustlist for the certificate or its chain.
            return true; // Placeholder return value
        } catch (Exception e) {
            throw new TrustlistException("Could not find out trustlist status ", e);
        }
    }

    private void validUri(URI uri) {
        if (uri == null ) {
            throw new IllegalArgumentException("Trustlist uri is null");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Trustlist uri must be https");
        }
    }
}