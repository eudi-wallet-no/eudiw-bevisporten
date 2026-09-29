package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Represents the {@code <Name xml:lang="..."><NonEmptyNormalizedString>...</NonEmptyNormalizedString></Name>}
 * structure used by ETSI TS 119 602 XML lists, where the localized text is nested one level deeper
 * (in its own child element) than in the ETSI TS 119 612 XML format.
 */
public record Name602Xml(
        @JacksonXmlProperty(localName = "lang", isAttribute = true)
        @NotBlank String lang,

        @JacksonXmlProperty(localName = "NonEmptyNormalizedString", namespace = Etsi602XmlNamespaces.LOTE)
        String value
) {
}
