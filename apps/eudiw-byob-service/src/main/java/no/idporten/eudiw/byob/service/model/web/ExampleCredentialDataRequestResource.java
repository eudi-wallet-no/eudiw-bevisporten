package no.idporten.eudiw.byob.service.model.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExampleCredentialDataRequestResource(
        @NotEmpty
        @JsonProperty("name")
        String name,
        @NotEmpty
        @JsonProperty("value")
        String value
){
    public ExampleCredentialData toExampleCredentialData() {
        return new ExampleCredentialData(
                this.name,
                this.value
        );
    }
}
