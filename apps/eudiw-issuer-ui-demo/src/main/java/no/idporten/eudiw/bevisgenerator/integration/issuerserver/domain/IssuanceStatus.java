package no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Contract for the statuses exposed by issuer-server. Keep this aligned with
 * CredentialIssuanceStatusResponse and NotificationRequest in eudiw-issuer-server.
 */
public enum IssuanceStatus {
    OFFER_ISSUED("offer_issued"),
    CREDENTIAL_ISSUED("credential_issued"),
    CREDENTIAL_ACCEPTED("credential_accepted"),
    CREDENTIAL_DELETED("credential_deleted"),
    CREDENTIAL_FAILURE("credential_failure"),
    UNKNOWN("unknown"),
    UNRECOGNIZED(null);

    private final String value;

    IssuanceStatus(String value) {
        this.value = value;
    }

    @JsonCreator
    public static IssuanceStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(status -> value.equals(status.value))
                .findFirst()
                .orElse(UNRECOGNIZED);
    }

    @JsonValue
    public String value() {
        return value;
    }
}
