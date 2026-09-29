package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Root of a raw signed XML trustlist in the ETSI TS 119 602 format (as opposed to the JSON-in-JWS
 * variant modelled by {@link no.idporten.eudiw.verifier.trustlist.etsi602.LoTEJson}). This format has
 * no {@code ServiceStatus}/status concept anywhere, so every trusted entity service is treated as active.
 */
@JacksonXmlRootElement(localName = "TrustedEntitiesList")
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoTEXml602(
        @JacksonXmlProperty(localName = "ListAndSchemeInformation", namespace = Etsi602XmlNamespaces.LOTE)
        @Valid @NotNull ListAndSchemeInformation602Xml schemeInformation,

        @JacksonXmlProperty(localName = "TrustedEntitiesList", namespace = Etsi602XmlNamespaces.LOTE)
        @Valid @NotNull TrustedEntitiesList602Xml trustedEntitiesList
) {
}
