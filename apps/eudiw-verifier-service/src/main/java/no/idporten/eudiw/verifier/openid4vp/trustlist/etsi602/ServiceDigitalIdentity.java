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
import java.util.ArrayList;
import java.util.List;

public record ServiceDigitalIdentity(
        @JsonProperty("X509Certificates")
        List<ValueCertificate> X509Certificates) {

    private static final Logger log = LoggerFactory.getLogger(ServiceDigitalIdentity.class);

    public List<X509Certificate> certListFromStringsToCerts() {
        List<X509Certificate> certs = new ArrayList<>();
        for(ValueCertificate cert : X509Certificates) {
            certs.add(cert.getCertificateAsX509Object());
        }
        return certs;
    }
}
