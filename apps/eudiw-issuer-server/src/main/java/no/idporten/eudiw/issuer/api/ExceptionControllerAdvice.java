package no.idporten.eudiw.issuer.api;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(IssuerServerException.class)
    public ResponseEntity<ErrorResponse> issuerServerException(IssuerServerException issuerServerException) {
        log.warn("Failed to process request", issuerServerException);
        return ResponseEntity
                .status(issuerServerException.getHttpStatus())
                .body(new ErrorResponse(issuerServerException.getError(), issuerServerException.getErrorDescription()));
    }

}
