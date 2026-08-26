package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

@JsonSerialize
public record ValidationDetail(ValidationType validationType, ValidationStatus status, Object validationDetails) implements Serializable {
}
