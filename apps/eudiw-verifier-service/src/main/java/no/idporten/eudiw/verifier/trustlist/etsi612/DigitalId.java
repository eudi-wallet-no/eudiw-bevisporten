package no.idporten.eudiw.verifier.trustlist.etsi612;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DigitalId {

    @JacksonXmlProperty(localName = "X509SubjectName", namespace = EtsiNamespaces.ETSI_TSL)
    private String x509SubjectName;

    @JacksonXmlProperty(localName = "X509Certificate", namespace = EtsiNamespaces.ETSI_TSL)
    private String cert;

    public DigitalId() {}

    public String getX509SubjectName() {
        return x509SubjectName;
    }

    public void setX509SubjectName(String x509SubjectName) {
        this.x509SubjectName = x509SubjectName;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public boolean hasSubjectName() {
        return x509SubjectName != null && !x509SubjectName.isBlank();
    }

    public boolean hasCertificate() {
        return cert != null && !cert.isBlank();
    }

    public String getValidCertString() {
        if (!hasCertificate()) {
            throw new IllegalStateException("DigitalId cert is null/blank for this DigitalId entry");
        }
        String prefix = "-----BEGIN CERTIFICATE-----";
        String suffix = "-----END CERTIFICATE-----";
        if (!cert.startsWith(prefix) || !cert.endsWith(suffix)) {
            return prefix + System.lineSeparator() + cert + System.lineSeparator() + suffix;
        }
        return cert;
    }

    public X509CertificateHolder getCertificate() throws IOException {
        try (PEMParser pemParser = new PEMParser(new StringReader(getValidCertString()))) {
            return (X509CertificateHolder) pemParser.readObject();
        }
    }

    public X509Certificate getCertificateAsX509Object() throws IOException, CertificateException {
        return new JcaX509CertificateConverter().getCertificate(getCertificate());
    }
}