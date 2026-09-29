package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrustedEntity602Xml(
        @JacksonXmlProperty(localName = "TrustedEntityServices", namespace = Etsi602XmlNamespaces.LOTE)
        @Valid @NotNull TrustedEntityServices602Xml trustedEntityServices
) {
}
