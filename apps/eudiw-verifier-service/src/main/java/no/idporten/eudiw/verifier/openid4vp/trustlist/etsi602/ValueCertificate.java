package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.verifier.VerificationException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public record ValueCertificate(
        @JsonProperty("val")
        String cert
) {


    private static final Logger log = LoggerFactory.getLogger(ValueCertificate.class);

    public String getValidCertString() {
        String prefix = "-----BEGIN CERTIFICATE-----";
        String suffix = "-----END CERTIFICATE-----";
        if (!cert.startsWith(prefix) || !cert.endsWith(suffix)) {
            return prefix + System.lineSeparator() +
                    cert + System.lineSeparator() +
                    suffix;
        }
        return cert;
    }

    public X509CertificateHolder getCertificate() {
        String validCert = getValidCertString();
        try (StringReader stringReader = new StringReader(validCert);
             PEMParser pemParser = new PEMParser(stringReader)) {
            return (X509CertificateHolder) pemParser.readObject();
        } catch (IOException e) {
            throw new VerificationException("Feil i lesing av x509 sertifikat i tillitsliste 602", e.getMessage());
        }
    }

    public X509Certificate getCertificateAsX509Object()  {
        try {
            return new JcaX509CertificateConverter().getCertificate(getCertificate());
        } catch (CertificateException e) {
            log.info("error parsing certificate", e);
        }
        return null;
    }
}
