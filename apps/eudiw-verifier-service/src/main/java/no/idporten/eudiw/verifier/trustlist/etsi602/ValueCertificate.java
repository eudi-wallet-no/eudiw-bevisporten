package no.idporten.eudiw.verifier.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.verifier.VerificationException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public record ValueCertificate(
        @JsonProperty("val")
        String cert
) {

    public boolean hasCertificate() {
        return cert != null && !cert.isBlank();
    }

    public String getValidCertString() {
        if (!hasCertificate()) {
            throw new VerificationException("invalid_request", "DigitalId cert is null/blank for this DigitalId entry");
        }
        String prefix = "-----BEGIN CERTIFICATE-----";
        String suffix = "-----END CERTIFICATE-----";
        if (!cert.startsWith(prefix) || !cert.endsWith(suffix)) {
            return prefix + System.lineSeparator() + cert + System.lineSeparator() + suffix;
        }
        return cert;
    }

    public X509CertificateHolder getCertificate() {
        try (PEMParser pemParser = new PEMParser(new StringReader(getValidCertString()))) {
            return (X509CertificateHolder) pemParser.readObject();
        } catch (IOException e) {
            throw new VerificationException("invalid_request", "Certificate cannot be read", e);
        }
    }

    public X509Certificate getCertificateAsX509Object() {
        try {
            return new JcaX509CertificateConverter().getCertificate(getCertificate());
        } catch (CertificateException e) {
            throw new VerificationException("invalid_request", "Certificate cannot be read into x509 object", e);
        }

    }
}
