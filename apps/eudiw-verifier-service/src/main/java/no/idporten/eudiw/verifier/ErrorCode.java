package no.idporten.eudiw.verifier;

import org.springframework.http.HttpStatus;

/**
 * Error codes and HTTP status codes for error responses.
 */
public enum ErrorCode {

    // https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#name-error-response
    INVALID_SCOPE("invalid_scope", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("invalid_request", HttpStatus.BAD_REQUEST),
    INVALID_CLIENT("invalid_client", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("access_denied", HttpStatus.FORBIDDEN),
    VP_FORMATS_NOT_SUPPORTED("vp_formats_not_supported", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_URI_METHOD("invalid_request_uri_method", HttpStatus.BAD_REQUEST),
    INVALID_TRANSACTION_DATA("invalid_transaction_data", HttpStatus.BAD_REQUEST),
    WALLET_UNAVAILABLE("wallet_unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    SERVER_ERROR("server_error", HttpStatus.INTERNAL_SERVER_ERROR),


    INVALID_RRESPONSE("invalid_response_external", HttpStatus.BAD_REQUEST); // Se næærmere på denne!!

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

    public static HttpStatus httpStatusFor(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return HttpStatus.BAD_REQUEST;
        }
        for (ErrorCode value : values()) {
            if (value.error.equals(errorCode)) {
                return value.httpStatus;
            }
        }
        return HttpStatus.BAD_REQUEST;
    }

    public static ErrorCode fromHttpStatus(HttpStatus httpStatus) {
        if (httpStatus == null) {
            return INVALID_REQUEST;
        }
        for (ErrorCode value : values()) {
            if (value.httpStatus.equals(httpStatus)) {
                return value;
            }
        }
        if (httpStatus.is4xxClientError()) {
            return INVALID_REQUEST;
        }
        if (httpStatus.is5xxServerError()) {
            return SERVER_ERROR;
        }
        return INVALID_REQUEST;
    }
}
