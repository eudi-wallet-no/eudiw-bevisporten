package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.VerificationException;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceDigitalIdentity602Xml(
        @JacksonXmlProperty(localName = "DigitalId", namespace = Etsi602XmlNamespaces.LOTE)
        @Valid @NotNull List<@NotNull DigitalId602Xml> digitalIds
) {
    public List<DigitalId602Xml> getCertificateDigitalIds() {
        List<DigitalId602Xml> listOfDigitalIdWithCertificate = new ArrayList<>();
        for (DigitalId602Xml digitalId : digitalIds) {
            if (digitalId.hasCertificate()) {
                if (digitalId.getCertificateAsX509Object() != null) {
                    listOfDigitalIdWithCertificate.add(digitalId);
                } else {
                    throw new VerificationException("invalid_request", "Certificate cannot be read into x509 object");
                }
            }
        }
        return listOfDigitalIdWithCertificate;
    }
}
