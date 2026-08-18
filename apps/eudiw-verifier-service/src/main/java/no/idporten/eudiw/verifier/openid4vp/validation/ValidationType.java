package no.idporten.eudiw.verifier.openid4vp.validation;

import tools.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public enum ValidationType {
    STATUS_LIST("statusList"),
    TRUST_LIST("trustlist");


    ValidationType(String value) {
        this.value = value;
    }

    private final String value;

    public String getValue() {
        return value;
    }
}
