package no.idporten.eudiw.verifier.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.verifier.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public record ServiceDigitalIdentity(
        @JsonProperty("X509Certificates")
        List<ValueCertificate> X509Certificates) {


    public List<X509Certificate> certListFromStringsToCerts() {
        List<X509Certificate> certs = new ArrayList<>();
        for(ValueCertificate cert : X509Certificates) {
            if(cert.getCertificateAsX509Object() != null) {
                certs.add(cert.getCertificateAsX509Object());
            } else {
                throw new VerificationException("invalid_request", "trustlist contains non-parsable certificate");
            }
        }
        return certs;
    }
}
