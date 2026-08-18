package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.signature;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record Transforms(
        @JacksonXmlProperty(localName = "Transform")
        @Valid @NotEmpty List<@NotBlank Algorithm> transforms
) {
}
