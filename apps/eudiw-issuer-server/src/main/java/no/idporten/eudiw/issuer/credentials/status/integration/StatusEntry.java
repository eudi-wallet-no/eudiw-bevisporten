package no.idporten.eudiw.issuer.credentials.status.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatusEntry(
        @JsonProperty("idx") int idx,
        @JsonProperty("uri") URI uri) implements Serializable {
}
