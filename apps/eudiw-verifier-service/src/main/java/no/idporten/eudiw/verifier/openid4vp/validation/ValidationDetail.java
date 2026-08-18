package no.idporten.eudiw.verifier.openid4vp.validation;

import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

@JsonSerialize
public record ValidationDetail(ValidationType validationType, ValidationStatus status, Object validationDetails) implements Serializable {
}
