package no.idporten.eudiw.bevisgenerator.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;

public class IssuerServerException extends RuntimeException {

    private final HttpStatusCodeException httpStatusCodeException;


     public IssuerServerException(String message, HttpStatusCodeException cause) {
        super(message, cause);
        httpStatusCodeException = cause;
    }

    public IssuerServerException(String message, RuntimeException cause) {
        super(message, cause);
        httpStatusCodeException = null;
    }

    public HttpStatusCode getHttpStatusCode() {
        return httpStatusCodeException.getStatusCode();
    }

    public String getCauseMessage(){
        return httpStatusCodeException.getMessage();
    }
}
