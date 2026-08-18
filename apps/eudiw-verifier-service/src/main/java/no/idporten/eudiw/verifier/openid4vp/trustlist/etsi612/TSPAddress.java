package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TSPAddress(

        @JacksonXmlProperty(localName = "PostalAddresses", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull PostalAddresses postalAddresses,

        @JacksonXmlProperty(localName = "ElectronicAddress", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull ElectronicAddress electronicAddress

) {}