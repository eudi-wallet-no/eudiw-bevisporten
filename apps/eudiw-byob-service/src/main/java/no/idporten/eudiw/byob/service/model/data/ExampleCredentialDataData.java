package no.idporten.eudiw.byob.service.model.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;

import java.io.Serializable;

public record ExampleCredentialDataData(
        @JsonProperty("name")
        String name,
        @JsonProperty("value")
        String value
) implements Serializable {

    public ExampleCredentialDataData(ExampleCredentialData exampleCredentialData) {
        this(exampleCredentialData.name(), exampleCredentialData.value());
    }

    public ExampleCredentialData toExampleCredentialData() {
        return new ExampleCredentialData(this.name, this.value);
    }
}
