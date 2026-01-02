package no.idporten.eudiw.byob.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExampleCredentialMetadata(
        @JsonProperty("foo")
        String foo
) {
}
