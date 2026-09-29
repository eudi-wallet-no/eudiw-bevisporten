package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceInformation602Xml(
        @JacksonXmlProperty(localName = "ServiceTypeIdentifier", namespace = Etsi602XmlNamespaces.LOTE)
        String serviceTypeIdentifier,

        @JacksonXmlProperty(localName = "ServiceDigitalIdentity", namespace = Etsi602XmlNamespaces.LOTE)
        @Valid @NotNull ServiceDigitalIdentity602Xml serviceDigitalIdentity
) {
}
