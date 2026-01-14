package no.idporten.eudiw.byob.service.model.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;

import java.io.Serializable;

public record ExampleCredentialDataData(
        @JsonProperty("json")
        String json
) implements Serializable {

    public ExampleCredentialDataData(ExampleCredentialData exampleCredentialData) {
        this(exampleCredentialData.json());
    }

    public ExampleCredentialData toExampleCredentialData() {
        return new ExampleCredentialData(this.json);
    }
}
