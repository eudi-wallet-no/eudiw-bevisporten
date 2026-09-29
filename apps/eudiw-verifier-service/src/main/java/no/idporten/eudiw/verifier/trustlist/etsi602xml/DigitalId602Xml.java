package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import no.idporten.eudiw.verifier.VerificationException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.StringReader;
import java.security.cert.X509Certificate;

/**
 * Certificate holder for the ETSI TS 119 602 XML trustlist format, mirroring
 * {@link no.idporten.eudiw.verifier.trustlist.etsi612.DigitalId}'s certificate parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DigitalId602Xml {

    @JacksonXmlProperty(localName = "X509Certificate", namespace = Etsi602XmlNamespaces.LOTE)
    private String cert;

    public DigitalId602Xml() {
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

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
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Certificate cannot be read", e);
        }
    }

    public X509Certificate getCertificateAsX509Object() {
        try {
            return new JcaX509CertificateConverter().getCertificate(getCertificate());
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Certificate cannot be read into x509 object", e);
        }
    }
}
