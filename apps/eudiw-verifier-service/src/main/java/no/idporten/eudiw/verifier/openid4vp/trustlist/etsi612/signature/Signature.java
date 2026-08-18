package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.signature;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.EtsiNamespaces;

public record Signature (
        @JacksonXmlProperty(localName = "SignedInfo", namespace = EtsiNamespaces.XMLDSIG)
        @Valid @NotNull SignedInfo signedInfo,
        @JacksonXmlProperty(localName = "KeyInfo", namespace = EtsiNamespaces.XMLDSIG)
        @Valid @NotNull KeyInfo keyInfo,
        @JacksonXmlProperty(localName = "SignatureValue", namespace = EtsiNamespaces.XMLDSIG)
        @NotBlank String signatureValue
) {
}
