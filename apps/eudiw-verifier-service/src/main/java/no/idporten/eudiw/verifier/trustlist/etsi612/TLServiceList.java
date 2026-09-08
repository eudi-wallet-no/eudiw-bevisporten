package no.idporten.eudiw.verifier.trustlist.etsi612;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TLServiceList(
        @JacksonXmlProperty(localName = "TSPService", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotEmpty List<@NotNull TSPService> services
) {}