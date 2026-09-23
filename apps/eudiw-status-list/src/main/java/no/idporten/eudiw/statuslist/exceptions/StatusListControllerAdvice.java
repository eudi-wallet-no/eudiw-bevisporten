package no.idporten.eudiw.statuslist.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

import static no.idporten.eudiw.statuslist.exceptions.ErrorCodes.INVALID_REQUEST;
import static no.idporten.eudiw.statuslist.exceptions.ErrorCodes.SERVER_ERROR;


@RestControllerAdvice
public class StatusListControllerAdvice {
    private final Logger log = LoggerFactory.getLogger(StatusListControllerAdvice.class);

    protected static ResponseEntity<ErrorResponse> errorResponseEntity(HttpStatusCode httpStatus, String error, String errorDescription) {
        return errorResponseEntity(httpStatus, new ErrorResponse(error, errorDescription));
    }

    // last resort
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Failed to process request", e);
        return errorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, "Server failed to process request");
    }

    @ExceptionHandler(StatusListException.class)
    public ResponseEntity<ErrorResponse> handleStatusProviderException(StatusListException e) {
        String logMessage = "%s: %s".formatted(e.getErrorCode(), e.getMessage());
        log.error(logMessage, e);
        return errorResponseEntity(e);
    }

    @ExceptionHandler(StatusListNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStatusEntryException(StatusListNotFoundException e) {
        return errorResponseEntity(e);
    }

    @ExceptionHandler(UnsupportedStatusException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedStatusException(UnsupportedStatusException e) {
        return errorResponseEntity(e);
    }

    @ExceptionHandler(StatusListBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleStatusListBadRequestException(StatusListBadRequestException e) {
        return errorResponseEntity(e);
    }

    // Validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Failed to process request", e);
        return errorResponseEntity(HttpStatus.BAD_REQUEST, INVALID_REQUEST, getFieldErrorDescription(e));
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
        return errorResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, INVALID_REQUEST, "Unsupported media type");
    }

    // Spring 406
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        return errorResponseEntity(HttpStatus.NOT_ACCEPTABLE, INVALID_REQUEST, "Unsupported Accept header, expected media type not acceptable");
    }

    // Spring 404
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException() {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring-exception som gir HTTP-feil
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        return errorResponseEntity(e.getStatusCode(), errorMessageForHttpStatus(e.getStatusCode()), e.getReason());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return errorResponseEntity(HttpStatus.BAD_REQUEST, INVALID_REQUEST, "Invalid argument");
    }

    protected String errorMessageForHttpStatus(HttpStatusCode httpStatus) {
        if (httpStatus.is4xxClientError()) {
            return INVALID_REQUEST;
        }
        return SERVER_ERROR;
    }

    protected ResponseEntity<ErrorResponse> errorResponseEntity(StatusListException e) {
        return errorResponseEntity(e.getStatusCode(), e.getErrorCode(), e.getMessage());
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
