package no.idporten.eudiw.byob.service.exception;


import no.idporten.eudiw.byob.service.model.web.CredentialConfigurationRequestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class ApplicationExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException e) {
        log.warn("Client error : {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("invalid_request", e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ForbiddenRequestException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenRequestException(ForbiddenRequestException e) {
        log.warn("Forbidden attempt to modify resource : {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("invalid_request", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(ByobServiceException.class)
    public ResponseEntity<ErrorResponse> handleByobServiceException(ByobServiceException e) {
        log.error("Internal error : " + e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse("internal_error", e.getMessage());
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder details = new StringBuilder();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.append("Attribute %s has error= %s\n".formatted(fieldError.getField(), fieldError.getDefaultMessage()));
        }

        String credentialType = "";
        if (ex.getBindingResult().getTarget() instanceof CredentialConfigurationRequestResource) {
            credentialType = ((CredentialConfigurationRequestResource) ex.getBindingResult().getTarget()).credentialType();
        }
        ErrorResponse errorResponse = new ErrorResponse("validation_error", details.toString());
        log.warn("Validation of input failed for credential type=%s: %s, %s".formatted(credentialType, errorResponse.error(), errorResponse.errorDescription())); // TODO: remove when in production with more traffic
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
