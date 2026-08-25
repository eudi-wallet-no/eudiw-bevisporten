package no.idporten.eudiw.verifier.trustlist.etsi612.signature;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;
import no.idporten.eudiw.verifier.trustlist.etsi612.EtsiNamespaces;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public record X509Data(
        @JacksonXmlProperty(localName = "X509Certificate", namespace = EtsiNamespaces.XMLDSIG)
        @NotBlank String x509Certificate
) {


    public X509CertificateHolder getCertificate(String cert) throws IOException {
        try (PEMParser pemParser = new PEMParser(new StringReader(x509Certificate))) {
            return (X509CertificateHolder) pemParser.readObject();
        }
    }

    public X509Certificate getCertificateAsX509Object() throws IOException, CertificateException {
        return new JcaX509CertificateConverter().getCertificate(getCertificate(x509Certificate));
    }
}
