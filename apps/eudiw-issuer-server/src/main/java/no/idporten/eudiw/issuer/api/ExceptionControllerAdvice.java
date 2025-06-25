package no.idporten.eudiw.issuer.api;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.openid4vci.protocol.InvalidProof;
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

    @ExceptionHandler(InvalidProof.class)
    public ResponseEntity<NonceErrorResponse> invalidProof(InvalidProof invalidProof) {
        log.warn("Failed to process request", invalidProof);
        return ResponseEntity
                .status(invalidProof.getHttpStatus())
                .body(new NonceErrorResponse(invalidProof.getError(), invalidProof.getErrorDescription(), invalidProof.getNonce(), invalidProof.getNonceExpiresInSecpnds()));
    }

}
