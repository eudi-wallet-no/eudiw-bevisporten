package no.idporten.eudiw.byob.service.model.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;

public record ExampleCredentialDataRequestResource(
        @JsonProperty("json")
        String json
) {
    public ExampleCredentialData toExampleCredentialData() {
        return new ExampleCredentialData(this.json);
    }
}
