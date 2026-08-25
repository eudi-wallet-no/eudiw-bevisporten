package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import tools.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public enum ValidationStatus {
    VALID("success"),
    INVALID("error"),
    INCONCLUSIVE("warning");

    private final String status;

    ValidationStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}