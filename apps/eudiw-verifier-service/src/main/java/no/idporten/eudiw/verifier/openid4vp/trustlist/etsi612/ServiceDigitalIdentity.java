package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceDigitalIdentity(
        @JacksonXmlProperty(localName = "DigitalId", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull List<@NotNull DigitalId> digitalIds
) {}
