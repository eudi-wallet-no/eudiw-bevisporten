package no.idporten.eudiw.issuer.claimssource.krr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonKrr(
        @JsonProperty("personidentifikator")
        String personidentifikator,
        @JsonProperty("kontaktinformasjon")
        Kontaktinformasjon kontaktinformasjon,
        @JsonProperty("digital_post")
        DigitalPost digitalpost
) {
}
