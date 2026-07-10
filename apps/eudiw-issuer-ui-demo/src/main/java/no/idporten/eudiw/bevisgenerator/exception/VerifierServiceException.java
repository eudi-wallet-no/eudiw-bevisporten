package no.idporten.eudiw.bevisgenerator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class VerifierServiceException extends RuntimeException {
    private HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

    public VerifierServiceException(String message) {
        super(message);
    }

     public VerifierServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public VerifierServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getHttpStatusCode() {
        return status;
    }
}
