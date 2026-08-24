package no.idporten.eudiw.bevisgenerator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;

public class IssuerServerException extends RuntimeException {

    private final HttpStatusCode httpStatusCode;
    private final String message;


    public IssuerServerException(String message, HttpStatusCodeException cause) {
        super(message, cause);
        this.httpStatusCode = cause.getStatusCode();
        this.message = cause.getMessage();
    }

    public IssuerServerException(String message, RuntimeException cause) {
        super(message, cause);
        this.httpStatusCode = HttpStatus.SERVICE_UNAVAILABLE;
        this.message = cause.getMessage();
    }

    public HttpStatusCode getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getCauseMessage(){
        return message;
    }
}
