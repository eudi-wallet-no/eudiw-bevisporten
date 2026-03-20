package no.idporten.eudiw.connector.authoritativesources.skatteetaten.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Inntekt(@JsonProperty("beloep") Double beloep) {
}
