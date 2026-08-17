package no.idporten.eudiw.connector.authoritativesources.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.validators.identifier.PersonIdentifier;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Subject(@JsonProperty("identifier") @PersonIdentifier(message = "Invalid person identifier") String identifier) {
}
