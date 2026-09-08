package no.idporten.eudiw.verifier.trustlist.etsi612;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TSName(
        @JacksonXmlProperty(localName = "Name")
        @Valid @NotEmpty List<@NotNull Localized> names
)
{}