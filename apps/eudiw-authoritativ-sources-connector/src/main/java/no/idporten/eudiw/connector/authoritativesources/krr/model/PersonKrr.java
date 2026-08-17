package no.idporten.eudiw.connector.authoritativesources.krr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonKrr(
        @JsonProperty("personidentifikator")
        String personidentifikator,
        @JsonProperty("reservasjon")
        String reservasjon,
        @JsonProperty("status")
        String status,
        @JsonProperty("varslingsstatus")
        String varslingsstatus,
        @JsonProperty("kontaktinformasjon")
        Kontaktinformasjon kontaktinformasjon
) {
}
