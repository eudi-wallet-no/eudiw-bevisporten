package no.idporten.eudiw.verifier.trustlist.etsi602xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.VerificationException;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListAndSchemeInformation602Xml(
        @JacksonXmlProperty(localName = "SchemeName", namespace = Etsi602XmlNamespaces.LOTE)
        @Valid @NotNull SchemeName602Xml schemeName,

        @JacksonXmlProperty(localName = "ListIssueDateTime", namespace = Etsi602XmlNamespaces.LOTE)
        @NotNull String listIssueDateTime
) {
    public ListAndSchemeInformation602Xml {
        if (listIssueDateTime == null || ZonedDateTime.parse(listIssueDateTime).isAfter(ZonedDateTime.now())) {
            throw new VerificationException("invalid_request", "Invalid list of scheme issue dates for trustlist");
        }
    }
}
