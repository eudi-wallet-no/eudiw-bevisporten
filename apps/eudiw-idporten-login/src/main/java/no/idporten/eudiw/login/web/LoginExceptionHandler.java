package no.idporten.eudiw.login.web;

import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.protocol.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class LoginExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoginExceptionHandler.class);

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<ErrorResponse> handleOAuth2Exception(OAuth2Exception exception) {
        logger.warn(exception.getMessage(), exception);
        return ResponseEntity.status(exception.getHttpStatusCode()).body(exception.errorResponse());
    }

}
