package no.idporten.eudiw.verifier.trustlist.etsi612;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TSPService(
        @JacksonXmlProperty(localName = "ServiceInformation", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TLService serviceInformation
) {}