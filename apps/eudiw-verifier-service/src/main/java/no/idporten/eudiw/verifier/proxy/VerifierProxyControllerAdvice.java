package no.idporten.eudiw.verifier.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class VerifierProxyControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(VerifierProxyControllerAdvice.class);

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<ErrorResponse> handleException(VerificationException e) {
        log.warn("Verification transaction failed", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getError(), e.getErrorDescription()));
    }

}
