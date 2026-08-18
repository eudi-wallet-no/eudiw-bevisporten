package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TSPInformation (
        @JacksonXmlProperty(localName = "TSPName", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TSName name,
        @JacksonXmlProperty(localName = "TSPTradeName", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TSName tradeName,
        @JacksonXmlProperty(localName = "TSPAddress", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TSPAddress tspAddress,
        @JacksonXmlProperty(localName = "TSPInformationURI", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TSUri informationUris

){
}
