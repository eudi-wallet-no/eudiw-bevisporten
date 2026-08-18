package no.idporten.eudiw.issuer.openid4vci.protocol;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.ErrorCode;

public class InvalidCredentialRequest extends IssuerServerException {

    public InvalidCredentialRequest(String errorDescription) {
        super(ErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
    }

}
