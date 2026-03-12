package no.idporten.eudiw.connector.authoritativesources.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RetrieveRequest(@JsonProperty("subject") @NotNull @Valid Subject subject) {
}
