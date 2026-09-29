package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Wraps the {@code <TrustedEntityServices>} element containing repeated {@code <TrustedEntityService>} children.
 */
public record TrustedEntityServices602Xml(
        @JacksonXmlProperty(localName = "TrustedEntityService", namespace = Etsi602XmlNamespaces.LOTE)
        @Valid @NotEmpty List<@NotNull TrustedEntityService602Xml> services
) {
}
