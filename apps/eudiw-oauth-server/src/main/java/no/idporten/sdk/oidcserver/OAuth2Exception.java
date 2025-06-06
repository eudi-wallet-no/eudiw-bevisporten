package no.idporten.sdk.oidcserver;

import no.idporten.sdk.oidcserver.protocol.ErrorResponse;

/**
 * Base exception class for OAuth2 related exceptions.  Provides an error response that can be sent to client.
 */
public class OAuth2Exception extends RuntimeException {

    public static final String INVALID_REQUEST = "invalid_request";
    public static final String INVALID_CLIENT = "invalid_client";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String INVALID_TOKEN = "invalid_token";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
    public static final String INVALID_SCOPE = "invalid_scope";
    public static final String INVALID_AUTHORIZATION_DETAILS = "invalid_authorization_details";
    public static final String SERVER_ERROR = "server_error";
    public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";

    private String error;
    private String errorDescription;
    private int httpStatusCode = 400;

    public OAuth2Exception(String error, String errorDescription, int httpStatusCode, Throwable t) {
        super(errorDescription == null ? error : errorDescription, t);
        this.error = error;
        this.errorDescription = errorDescription;
        this.httpStatusCode = httpStatusCode;
    }

    public OAuth2Exception(String error, String errorDescription, int httpStatusCode) {
        this(error, errorDescription, httpStatusCode, null);
    }


    public ErrorResponse errorResponse() {
        return ErrorResponse.builder().error(error).errorDescription(errorDescription).build();
    }

    public String error() {
        return error;
    }

    public String errorDescription() {
        return errorDescription;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

}
