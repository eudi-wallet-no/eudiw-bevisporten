package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SchemeName602Xml(
        @JacksonXmlProperty(localName = "Name", namespace = Etsi602XmlNamespaces.LOTE)
        @Valid @NotEmpty List<@NotNull Name602Xml> names
) {
}
