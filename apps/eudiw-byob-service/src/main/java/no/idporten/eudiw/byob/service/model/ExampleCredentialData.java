package no.idporten.eudiw.byob.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExampleCredentialData(
        @JsonProperty("name")
        String name,
        @JsonProperty("value")
        String value
) {
}
