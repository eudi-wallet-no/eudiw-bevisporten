package no.idporten.eudiw.verifier.openid4vp.validation;

import tools.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public enum ValidationStatus {
    VALID("success"),
    INVALID("error"),
    INCONCLUSIVE("warning"),
    NON_VERIFIABLE("success"); // for use for example when no status is provided at all in proof, and the proof is not
    // supposed to contain a status. This is then still a valid proof, but for validation details it should have its own message.

    private final String status;

    ValidationStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}
