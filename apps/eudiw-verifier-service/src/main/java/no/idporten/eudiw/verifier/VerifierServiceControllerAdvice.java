package no.idporten.eudiw.verifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class VerifierServiceControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(VerifierServiceControllerAdvice.class);

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<ErrorResponse> handleException(VerificationException e) {
        log.warn("Verification transaction failed", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getError(), e.getErrorDescription()));
    }

    @ExceptionHandler(IOConnectionException.class)
    public ResponseEntity<ErrorResponse> handleException(IOConnectionException e) {
        log.warn("Connect to external service failed", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse("internal_error", e.getErrorDescription()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.warn("Unknown error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("internal_error", e.getMessage()));
    }

}
