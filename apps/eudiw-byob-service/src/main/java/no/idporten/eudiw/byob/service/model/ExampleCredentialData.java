package no.idporten.eudiw.byob.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExampleCredentialData(
        @JsonProperty("foo")
        String foo
) {
}
