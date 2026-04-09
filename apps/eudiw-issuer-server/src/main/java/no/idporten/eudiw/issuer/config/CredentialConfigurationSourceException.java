package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.exception.ErrorCode;

public class CredentialConfigurationSourceException extends IssuerServerException {

    private final String source;

    public CredentialConfigurationSourceException(String source, String errorDescription, Throwable cause) {
        super(ErrorCode.SERVER_ERROR, errorDescription, cause);
        this.source = source;
    }

    public CredentialConfigurationSourceException(String source, String errorDescription, String logMessage) {
        super(ErrorCode.SERVER_ERROR, errorDescription, logMessage);
        this.source = source;
    }

    public String source() {
        return source;
    }

}
