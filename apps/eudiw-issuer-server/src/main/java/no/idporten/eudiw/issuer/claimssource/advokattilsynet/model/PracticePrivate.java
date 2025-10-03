package no.idporten.eudiw.issuer.claimssource.advokattilsynet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PracticePrivate(
        @JsonProperty("organisasjonsNummer")
        long organisasjonsNummer,
        @JsonProperty("hovedpraksis")
        boolean hovedpraksis)
{}
