package no.idporten.eudiw.oauthserver.proxy;

import no.idporten.sdk.oidcserver.OAuth2Exception;
import org.springframework.http.HttpStatus;

public class OIDCProxyException extends OAuth2Exception {

    public OIDCProxyException(String error, String errorDescription, HttpStatus httpStatus, Throwable t) {
        super(error, errorDescription, httpStatus.value(), t);
    }

    public OIDCProxyException(String error, String errorDescription, HttpStatus httpStatus) {
        super(error, errorDescription, httpStatus.value());
    }
}
