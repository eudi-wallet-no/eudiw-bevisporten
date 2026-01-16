package no.idporten.eudiw.issuer.ui.demo.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

public class ByobServiceException extends RuntimeException {

    private final RestClientResponseException restClientResponseException;

    public ByobServiceException(String message) {
        super(message);
        restClientResponseException = null;
    }

     public ByobServiceException(String message, RestClientResponseException cause) {
        super(message, cause);
        restClientResponseException = cause;
    }

    public HttpStatusCode getHttpStatusCode() {
        return restClientResponseException.getStatusCode();
    }

    public String getCauseMessage(){
        return restClientResponseException.getMessage();
    }
}
