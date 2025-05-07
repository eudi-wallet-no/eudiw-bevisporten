package no.idporten.eudiw.issuer.openid4vci;

import lombok.Getter;

@Getter
public class InvalidCredentialRequest extends RuntimeException {

    public static final String INVALID_CREDENTIAL_REQUEST = "invalid_credential_request";

    private final String error;

    public InvalidCredentialRequest(String error, String errorDescription) {
        super(errorDescription);
        this.error = error;
    }

    public String getErrorDescription() {
        return super.getMessage();
    }

}
