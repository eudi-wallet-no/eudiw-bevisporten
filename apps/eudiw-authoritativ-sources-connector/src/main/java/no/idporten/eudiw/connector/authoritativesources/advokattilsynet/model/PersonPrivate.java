package no.idporten.eudiw.connector.authoritativesources.advokattilsynet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonPrivate(
        @JsonProperty("fornavn")
        String fornavn,
        @JsonProperty("mellomnavn")
        String mellomnavn,
        @JsonProperty("etternavn")
        String etternavn,
        @JsonProperty("tittel")
        String tittel,
        @JsonProperty("regnr")
        String regnr,
        @JsonProperty("tilknyttedePraksiser")
        List<PracticePrivate> tilknyttedePraksiser
) {
}
