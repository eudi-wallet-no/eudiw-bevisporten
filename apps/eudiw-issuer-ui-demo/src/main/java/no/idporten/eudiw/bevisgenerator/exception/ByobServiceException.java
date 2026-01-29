package no.idporten.eudiw.bevisgenerator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientException;

public class ByobServiceException extends RuntimeException {

    private final RestClientException restClientResponseException;

    public ByobServiceException(String message) {
        super(message);
        restClientResponseException = null;
    }

     public ByobServiceException(String message, RestClientException cause) {
        super(message, cause);
        restClientResponseException = cause;
    }

    public HttpStatusCode getHttpStatusCode() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    public String getCauseMessage(){
        return restClientResponseException.getMessage();
    }
}
