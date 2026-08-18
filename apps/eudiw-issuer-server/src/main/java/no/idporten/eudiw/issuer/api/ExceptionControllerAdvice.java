package no.idporten.eudiw.issuer.api;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.api.openid4vci.NonceErrorResponse;
import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSourceIOException;
import no.idporten.eudiw.issuer.claimssource.exception.CredentialRequestDeniedException;
import no.idporten.eudiw.issuer.openid4vci.proofs.InvalidProof;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class ExceptionControllerAdvice {


    @ExceptionHandler(InvalidProof.class)
    public ResponseEntity<NonceErrorResponse> invalidProof(InvalidProof invalidProof) {
        log.warn("Failed to process request", invalidProof);
        return ResponseEntity
                .status(invalidProof.getHttpStatus())
                .body(new NonceErrorResponse(invalidProof.getError(), invalidProof.getErrorDescription(), invalidProof.getNonce(), invalidProof.getNonceExpiresInSeconds()));
    }

    @ExceptionHandler(CredentialRequestDeniedException.class)
    public ResponseEntity<ErrorResponse> credentialRequestDeniedException(CredentialRequestDeniedException credentialRequestDeniedException) {
        if(credentialRequestDeniedException.getLogMessage() != null) {
            log.error(credentialRequestDeniedException.getLogMessage(), credentialRequestDeniedException);
        }else {
            log.warn("Request denied for credential configuration {}", credentialRequestDeniedException.credentialConfigurationId(), credentialRequestDeniedException);
        }
        return ResponseEntity
                .status(credentialRequestDeniedException.getHttpStatus())
                .body(new ErrorResponse(credentialRequestDeniedException.getError(), credentialRequestDeniedException.getErrorDescription()));
    }

    @ExceptionHandler(AuthoritativeSourceIOException.class)
    public ResponseEntity<ErrorResponse> authoritativeSourceIOException(AuthoritativeSourceIOException exception) {
        // TODO metrics for this type of exception
        if(exception.getLogMessage() != null) {
            log.error(exception.getLogMessage(), exception);
        }else {
            log.warn("Request failed for authoritative source {}", exception.credentialConfigurationId(), exception);
        }
        return ResponseEntity
                .status(exception.getHttpStatus())
                .body(new ErrorResponse(exception.getError(), exception.getErrorDescription()));
    }

    @ExceptionHandler(IssuerServerException.class)
    public ResponseEntity<ErrorResponse> issuerServerException(IssuerServerException issuerServerException) {
        log.error(issuerServerException.getLogMessage() != null ? issuerServerException.getLogMessage() : "Failed to process request", issuerServerException);
        return ResponseEntity
                .status(issuerServerException.getHttpStatus())
                .body(new ErrorResponse(issuerServerException.getError(), issuerServerException.getErrorDescription()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> runtimeException(RuntimeException runtimeException) {
        log.error("Failed to process request", runtimeException);
        return ResponseEntity
                .status(500)
                .body(new ErrorResponse("server_error", "The server encountered an unexpected condition that prevented it from fulfilling the request"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Failed to process request", e);
        return ResponseEntity
                .status(e.getStatusCode())
                .body(new ErrorResponse("invalid_request", e.getBindingResult().getFieldError().getDefaultMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Failed to process request", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("invalid_request", "HTTP request not readable."));
    }

}
