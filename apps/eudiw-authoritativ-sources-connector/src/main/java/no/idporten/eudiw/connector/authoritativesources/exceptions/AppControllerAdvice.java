package no.idporten.eudiw.connector.authoritativesources.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.INVALID_REQUEST;
import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.SERVER_ERROR;

@RestControllerAdvice
public class AppControllerAdvice {
    Logger log = LoggerFactory.getLogger(AppControllerAdvice.class);

    // last resort
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Failed to process request", e);
        return errorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, "Server failed to process request");
    }

    @ExceptionHandler(AuthoritativeSourceException.class)
    public ResponseEntity<ErrorResponse> handleAuthoritativeSourceException(AuthoritativeSourceException exception) {
        return errorResponseEntity(exception.getStatusCode(), exception.getErrorCode(), exception.getMessage());
    }

    // Validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        log.warn("Failed to process request", exception);
        return errorResponseEntity(HttpStatus.BAD_REQUEST, INVALID_REQUEST, getFieldErrorDescription(exception));
    }

    // Spring 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return errorResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, INVALID_REQUEST, "Unsupported HTTP method");
    }

    // Spring 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return errorResponseEntity(HttpStatus.BAD_REQUEST, INVALID_REQUEST, "Malformed request");
    }

    // Spring 415
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        return errorResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, INVALID_REQUEST, "Malformed request");
    }

    // Spring 404
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring 404
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring-exception som gir HTTP-feil
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        return errorResponseEntity(e.getStatusCode(), errorMessageForHttpStatus(e.getStatusCode()), e.getReason());
    }

    protected String errorMessageForHttpStatus(HttpStatusCode httpStatus) {
        if (httpStatus.is4xxClientError()) {
            return INVALID_REQUEST;
        }
        return SERVER_ERROR;
    }

    protected static ResponseEntity<ErrorResponse> errorResponseEntity(HttpStatusCode httpStatus, String error, String errorDescription) {
        return errorResponseEntity(httpStatus, new ErrorResponse(error, errorDescription));
    }

    protected static ResponseEntity<ErrorResponse> errorResponseEntity(HttpStatusCode httpStatus, ErrorResponse errorResponse) {
        return ResponseEntity
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    private static String getFieldErrorDescription(MethodArgumentNotValidException exception) {
        FieldError error = exception.getBindingResult().getFieldError();
        return (error != null)
                ? error.getDefaultMessage()
                : "Invalid request";
    }
}
