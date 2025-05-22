package no.idporten.eudiw.issuer;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IssuerServerException extends RuntimeException {

    public static final String INVALID_CREDENTIAL_REQUEST = "invalid_credential_request";

    private final String error;
    private HttpStatus httpStatus;

    public IssuerServerException(String error, String errorDescription, HttpStatus httpStatus) {
        this(error, errorDescription, httpStatus, null);
    }

    public IssuerServerException(String error, String errorDescription, HttpStatus httpStatus, Throwable cause) {
        super(errorDescription, cause);
        this.error = error;
        this.httpStatus = httpStatus;
    }

    public String getErrorDescription() {
        return super.getMessage();
    }

}
