package no.idporten.eudiw.oauthserver.api;

import lombok.extern.slf4j.Slf4j;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.protocol.ErrorResponse;
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
