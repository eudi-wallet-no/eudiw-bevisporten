package no.idporten.eudiw.verifier.trustlist.etsi612.signature;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.trustlist.etsi612.EtsiNamespaces;

public record SignedInfo(
        @JacksonXmlProperty(localName = "CanonicalizationMethod", namespace = EtsiNamespaces.XMLDSIG)
        @NotBlank Algorithm canonicalizationMethod,
        @JacksonXmlProperty(localName = "SignatureMethod", namespace = EtsiNamespaces.XMLDSIG)
        @NotBlank Algorithm signatureMethod,
        @JacksonXmlProperty(localName = "Reference", namespace = EtsiNamespaces.XMLDSIG)
        @Valid @NotNull Reference reference

) {
}
