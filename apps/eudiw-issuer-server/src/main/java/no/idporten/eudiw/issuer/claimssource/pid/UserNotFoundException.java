package no.idporten.eudiw.issuer.claimssource.pid;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends RuntimeException {

    private HttpStatus status = HttpStatus.NOT_FOUND;

    public UserNotFoundException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }
}
