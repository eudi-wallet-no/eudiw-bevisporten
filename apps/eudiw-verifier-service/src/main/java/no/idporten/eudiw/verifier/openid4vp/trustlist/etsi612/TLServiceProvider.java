package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TLServiceProvider(
        @JacksonXmlProperty(localName= "TSPInformation", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TSPInformation tspInformation,
        @JacksonXmlProperty(localName = "TSPServices", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TLServiceList services
)
{
}