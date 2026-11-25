package net.eidas2sandkasse.tsl;

import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Status at an index in a verified token status list.
 *
 * @param status            0 VALID, 1 INVALID, 2 SUSPENDED, other values are application specific.  See https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/ .
 * @param signerCertificate leaf certificate the list was signed with
 */
public record TokenStatus(int status, StatusListFormat format, X509Certificate signerCertificate) {

    public TokenStatus {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(signerCertificate, "signerCertificate");
    }

    public boolean isValid() {
        return status == 0;
    }

}
