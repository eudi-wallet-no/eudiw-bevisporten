package no.idporten.eudiw.verifier.trustlist.etsi612;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.trustlist.etsi612.signature.Signature;


@JacksonXmlRootElement(namespace = EtsiNamespaces.ETSI_TSL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoTE (
        @JacksonXmlProperty(localName = "SchemeInformation", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TLSchemeInformation schemeInformation,

        @JacksonXmlProperty(localName = "TrustServiceProviderList", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TLServiceProviderList serviceProviderList,

        @JacksonXmlProperty(localName = "Signature", namespace = EtsiNamespaces.XMLDSIG)
        @Valid @NotNull Signature signature
)
{
}