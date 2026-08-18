package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PostalAddresses(

        @JacksonXmlProperty(localName = "PostalAddress", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull Address postalAddress

) {}