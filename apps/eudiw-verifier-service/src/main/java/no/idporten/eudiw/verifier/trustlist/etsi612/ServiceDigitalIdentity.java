package no.idporten.eudiw.verifier.trustlist.etsi612;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.VerificationException;

import java.util.ArrayList;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceDigitalIdentity(
        @JacksonXmlProperty(localName = "DigitalId", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull List<@NotNull DigitalId> digitalIds
) {
    public List<DigitalId> getCertificateDigitalIds() {
        List<DigitalId> listOfDigitalIdWithCertificate = new ArrayList<>();
        for(DigitalId digitalId : digitalIds) {
            if(digitalId.hasCertificate()) {
                if(digitalId.getCertificateAsX509Object() != null) {
                    listOfDigitalIdWithCertificate.add(digitalId);
                } else {
                    throw new VerificationException("invalid_request", "Certificate cannot be read into x509 object");
                }
            }
        }
        return  listOfDigitalIdWithCertificate;
    }
}
