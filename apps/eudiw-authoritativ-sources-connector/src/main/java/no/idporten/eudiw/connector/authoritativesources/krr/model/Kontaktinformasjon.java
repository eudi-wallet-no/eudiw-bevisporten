package no.idporten.eudiw.connector.authoritativesources.krr.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public record Kontaktinformasjon(
        @JsonProperty("epostadresse")
        String epostadresse,
        @JsonProperty("mobiltelefonnummer")
        String mobiltelefonnummer
) {
}