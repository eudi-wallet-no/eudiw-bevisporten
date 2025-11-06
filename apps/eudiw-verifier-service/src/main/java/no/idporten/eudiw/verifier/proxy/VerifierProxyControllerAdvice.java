package no.idporten.eudiw.verifier.proxy;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class VerifierProxyControllerAdvice {

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<ErrorResponse> handleException(VerificationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getError(), e.getErrorDescription()));
    }

}
