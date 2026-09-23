package no.idporten.eudiw.login.openid4vp.verifier.model;

public enum VerificationStatus {
    WAIT,
    AVAILABLE,
    ERROR,
    UNKNOWN;

    public static VerificationStatus fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }

    public boolean isTerminal() {
        return this == AVAILABLE || this == ERROR;
    }
}
