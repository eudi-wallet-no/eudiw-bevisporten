package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

@JsonSerialize
public record ValidationDetail(
        @JsonProperty("validationType")
        ValidationType validationType,
        @JsonProperty("status")
        ValidationStatus status,
        @JsonProperty("validationDetails")
        Object validationDetails
) implements Serializable {
}
