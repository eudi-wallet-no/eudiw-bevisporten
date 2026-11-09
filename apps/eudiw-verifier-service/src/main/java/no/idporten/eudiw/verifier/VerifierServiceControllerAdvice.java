package no.idporten.eudiw.verifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class VerifierServiceControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(VerifierServiceControllerAdvice.class);

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<ErrorResponse> verificationException(VerificationException e) {
        if (e.getLogMessage() != null) {
            log.error(e.getLogMessage(), e);
        } else if (e.getErrorDescription() != null) {
            log.error("Verification transaction failed with error: {} and description: {}", e.getError(), e.getErrorDescription(), e);
        } else {
            log.warn("Verification transaction failed", e);
        }

        HttpStatus status = e.getHttpStatus();
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(e.getError(), e.getErrorDescription()));
    }

    @ExceptionHandler(IOConnectionException.class)
    public ResponseEntity<ErrorResponse> ioConnectionException(IOConnectionException e) {
        log.warn("Connect to external service failed", e);
        HttpStatus status = e.getHttpStatus();
        if (status == null) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(e.getErrorCode(), e.getErrorDescription()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> responseStatusException(ResponseStatusException e) {
        log.warn("Unknown error", e);
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }

        ErrorCode errorCode = ErrorCode.fromHttpStatus(status);
        String description = e.getReason();
        if (description == null) {
            description = "Request failed";
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(errorCode.error(), description));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unknownException(Exception e) {
        log.warn("Unknown error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(ErrorCode.SERVER_ERROR.error(), "The server encountered an unexpected condition that prevented it from fulfilling the request"));
    }
}
