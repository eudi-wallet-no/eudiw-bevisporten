package no.idporten.eudiw.login.web;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletResponse;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.protocol.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

import static no.idporten.sdk.oidcserver.OAuth2Exception.INVALID_REQUEST;
import static no.idporten.sdk.oidcserver.OAuth2Exception.SERVER_ERROR;

@ControllerAdvice
public class LoginExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoginExceptionHandler.class);

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<ErrorResponse> handleOAuth2Exception(OAuth2Exception exception) {
        logger.warn(exception.getMessage(), exception);
        return ResponseEntity.status(exception.getHttpStatusCode()).body(exception.errorResponse());
    }

    // Spring 404
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNoHandlerFoundException(NoHandlerFoundException e, Model model, HttpServletResponse response) {
        return prepareErrorView(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found", model, response);
    }

    // Spring 404
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResourceFoundException(NoResourceFoundException e, Model model, HttpServletResponse response) {
        return prepareErrorView(HttpStatus.NOT_FOUND, INVALID_REQUEST, enrichWithTraceId("Requested resource not found"), model, response);
    }

    // Spring 404
    @ExceptionHandler(NoSuchElementException.class)
    public String handleNoSuchElementException(NoSuchElementException e, Model model, HttpServletResponse response) {
        return prepareErrorView(HttpStatus.NOT_FOUND, INVALID_REQUEST, enrichWithTraceId("Requested resource not found"), model, response);
    }

    // Spring-exception som gir HTTP-feil
    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(ResponseStatusException e, Model model, HttpServletResponse response) {
        return prepareErrorView(e.getStatusCode(), errorMessageForHttpStatus(e.getStatusCode()), e.getReason(), model, response);
    }

    // Spring 404
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model, HttpServletResponse response) {
        logger.error("Failed to process request", e);
        return prepareErrorView(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, "Server error", model, response);
    }

    // Spring 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, Model model, HttpServletResponse response) {
        return prepareErrorView(HttpStatus.METHOD_NOT_ALLOWED, INVALID_REQUEST, "Method not allowed", model, response);
    }

    protected String prepareErrorView(HttpStatusCode httpStatus, String error, String errorDescription, Model model, HttpServletResponse response) {
        response.setStatus(httpStatus.value());
        model.addAttribute("status", httpStatus.value());
        model.addAttribute("error", ErrorResponse.builder().error(error).errorDescription(errorDescription).build());
        return "error";
    }

    protected String errorMessageForHttpStatus(HttpStatusCode httpStatus) {
        if (httpStatus.is4xxClientError()) {
            return INVALID_REQUEST;
        }
        return SERVER_ERROR;
    }

    protected String enrichWithTraceId(String text) {
        String traceId = getTraceId();
        if (StringUtils.hasText(traceId)) {
            return "%s (traceId=%s)".formatted(text, traceId).trim();
        } else {
            return text;
        }
    }

    protected String getTraceId() {
        return Span.current().getSpanContext().getTraceId();
    }

}
