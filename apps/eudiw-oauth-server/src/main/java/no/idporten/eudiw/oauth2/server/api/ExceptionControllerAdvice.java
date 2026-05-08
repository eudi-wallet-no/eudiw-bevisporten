package no.idporten.eudiw.oauth2.server.api;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauth2.server.OAuth2Exception;
import no.idporten.eudiw.oauth2.server.protocol.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<ErrorResponse> handleOAuth2Exception(OAuth2Exception exception) {
        log.warn(exception.getMessage(), exception);
        return ResponseEntity.status(exception.getHttpStatusCode()).body(exception.errorResponse());
    }

}
