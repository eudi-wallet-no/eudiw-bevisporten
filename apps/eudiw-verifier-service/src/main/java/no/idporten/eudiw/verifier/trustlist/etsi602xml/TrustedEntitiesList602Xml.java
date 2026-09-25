package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Wraps the nested {@code <TrustedEntitiesList>} element (same local name as the document root) containing
 * repeated {@code <TrustedEntity>} children.
 */
public record TrustedEntitiesList602Xml(
        @JacksonXmlProperty(localName = "TrustedEntity", namespace = Etsi602XmlNamespaces.LOTE)
        @Valid @NotEmpty List<@NotNull TrustedEntity602Xml> trustedEntities
) {
}
