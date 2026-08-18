package no.idporten.eudiw.verifier.openid4vp.validation;

import tools.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public record ValidationDetail(ValidationType validationType, ValidationStatus status, Object validationDetails) {
}
