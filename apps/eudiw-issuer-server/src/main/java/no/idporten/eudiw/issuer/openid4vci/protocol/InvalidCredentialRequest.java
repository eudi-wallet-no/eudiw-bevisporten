package no.idporten.eudiw.issuer.openid4vci.protocol;

import lombok.Getter;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidCredentialRequest extends IssuerServerException {

    public static final String INVALID_CREDENTIAL_REQUEST = "invalid_credential_request";

    public InvalidCredentialRequest(String error, String errorDescription) {
        super(error, errorDescription, HttpStatus.BAD_REQUEST);
    }

}
