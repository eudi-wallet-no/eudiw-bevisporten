package no.idporten.eudiw.issuer.claimssource.exception;

import no.idporten.eudiw.issuer.IssuerServerException;

import static no.idporten.eudiw.issuer.claimssource.exception.ErrorCode.CREDENTIAL_REQUEST_DENIED;

/**
 * The credential request is denied.  See https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-error-response .
 */
public class CredentialRequestDeniedException extends IssuerServerException {

    private final String credentialConfigurationId;

    public CredentialRequestDeniedException(String credentialConfigurationId, String errorDescription) {
        super(CREDENTIAL_REQUEST_DENIED, errorDescription);
        this.credentialConfigurationId = credentialConfigurationId;
    }

    public CredentialRequestDeniedException(String credentialConfigurationId, String errorDescription, String logMessage) {
        super(CREDENTIAL_REQUEST_DENIED, errorDescription, logMessage);
        this.credentialConfigurationId = credentialConfigurationId;
    }

    public CredentialRequestDeniedException(String credentialConfigurationId, String errorDescription, Throwable cause) {
        super(CREDENTIAL_REQUEST_DENIED, errorDescription, cause);
        this.credentialConfigurationId = credentialConfigurationId;
    }

    public String credentialConfigurationId() {
        return credentialConfigurationId;
    }

}
