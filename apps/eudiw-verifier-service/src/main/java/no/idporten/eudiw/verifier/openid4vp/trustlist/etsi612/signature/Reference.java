package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.signature;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.EtsiNamespaces;

public record Reference(
        @JacksonXmlProperty(localName = "URI", isAttribute = true)
        String uri,
        @JacksonXmlProperty(localName = "Transforms", namespace = EtsiNamespaces.XMLDSIG)
        @Valid @NotNull Transforms transforms,
        @JacksonXmlProperty(localName = "DigestMethod", namespace = EtsiNamespaces.XMLDSIG)
        @Valid @NotNull Algorithm digestMethod,
        @JacksonXmlProperty(localName = "DigestValue", namespace = EtsiNamespaces.XMLDSIG)
        @NotBlank String digestValue
) {
}
