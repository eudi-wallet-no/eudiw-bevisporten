package no.idporten.eudiw.issuer.api;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceIOException;
import no.idporten.eudiw.issuer.openid4vci.protocol.InvalidProof;
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
                .body(new NonceErrorResponse(invalidProof.getError(), invalidProof.getErrorDescription(), invalidProof.getNonce(), invalidProof.getNonceExpiresInSecpnds()));
    }

    @ExceptionHandler(ClaimsSourceIOException.class)
    public ResponseEntity<ErrorResponse> claimSourceIOException(ClaimsSourceIOException claimSourceIOException) {
        log.error("IOException against claims-source=%s".formatted(claimSourceIOException.getAuthoritativeSource()), claimSourceIOException);
        // TODO: add metrics for IOExceptions
        return ResponseEntity
                .status(claimSourceIOException.getHttpStatus())
                .body(new ErrorResponse(claimSourceIOException.getError(), claimSourceIOException.getErrorDescription()));
    }

    @ExceptionHandler(ClaimsSourceDataNotFoundException.class)
    public ResponseEntity<ErrorResponse> claimSourceDataNotFoundException(ClaimsSourceDataNotFoundException claimsSourceDataNotFoundException) {
        if(claimsSourceDataNotFoundException.getLogMessage() != null) {
            log.error(claimsSourceDataNotFoundException.getLogMessage(), claimsSourceDataNotFoundException);
        }else {
            log.warn("Failed to find data in claims-source from authoritative source {}", claimsSourceDataNotFoundException.getAuthoritativeSource(), claimsSourceDataNotFoundException);
        }
        return ResponseEntity
                .status(claimsSourceDataNotFoundException.getHttpStatus())
                .body(new ErrorResponse(claimsSourceDataNotFoundException.getError(), claimsSourceDataNotFoundException.getErrorDescription()));
    }

    @ExceptionHandler(ClaimsSourceException.class)
    public ResponseEntity<ErrorResponse> claimSourceException(ClaimsSourceException claimSourceException) {
        if(claimSourceException.getLogMessage() != null) {
            log.error(claimSourceException.getLogMessage(), claimSourceException);
        }else {
            log.error("Failed to process request in claims-source", claimSourceException);
        }
        return ResponseEntity
                .status(claimSourceException.getHttpStatus())
                .body(new ErrorResponse(claimSourceException.getError(), claimSourceException.getErrorDescription()));
    }

    @ExceptionHandler(IssuerServerException.class)
    public ResponseEntity<ErrorResponse> issuerServerException(IssuerServerException issuerServerException) {
        log.error("Failed to process request", issuerServerException);
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
