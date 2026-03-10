package no.idporten.eudiw.connector.authoritativsources.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RetrieveRequest(@JsonProperty("subject") Subject subject) {
}
