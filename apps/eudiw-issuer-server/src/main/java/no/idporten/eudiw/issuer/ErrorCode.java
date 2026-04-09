package no.idporten.eudiw.issuer;

import org.springframework.http.HttpStatus;

/**
 * Error codes and HTTP status codes for error responses.
 */
public enum ErrorCode {

    // https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-10
    INVALID_REQUEST("invalid_request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_INVALID_REQUEST("invalid_request", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("invalid_token", HttpStatus.UNAUTHORIZED),
    INSUFFICIENT_SCOPE("insufficient_scope", HttpStatus.FORBIDDEN),
    SERVER_ERROR("server_error", HttpStatus.INTERNAL_SERVER_ERROR),

    // https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html
    CREDENTIAL_REQUEST_DENIED("credential_request_denied", HttpStatus.BAD_REQUEST),
    INVALID_PROOF("invalid_proof", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIAL_REQUEST("invalid_credential_request", HttpStatus.BAD_REQUEST),
    UNKNOWN_CREDENTIAL_IDENTIFIER("unknown_credential_identifier", HttpStatus.BAD_REQUEST),
    INVALID_NOTIFICATION_REQUEST("invalid_notification_request", HttpStatus.BAD_REQUEST),
    INVALID_NOTIFICATION_ID("invalid_notification_id", HttpStatus.BAD_REQUEST),

    // https://www.ietf.org/archive/id/draft-ietf-oauth-dpop-13.html
    INVALID_DPOP_PROOF("invalid_dpop_proof", HttpStatus.UNAUTHORIZED)
    ;

    private final String error;
    private final HttpStatus httpStatus;

    ErrorCode(String error, HttpStatus httpStatus) {
        this.error = error;
        this.httpStatus = httpStatus;
    }

    public String error() {
        return error;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
