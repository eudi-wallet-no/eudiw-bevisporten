package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import tools.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public enum ValidationType {
    STATUS_LIST("statusList"),
    TRUST_LIST("trustlist");


    private final String value;

    ValidationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
