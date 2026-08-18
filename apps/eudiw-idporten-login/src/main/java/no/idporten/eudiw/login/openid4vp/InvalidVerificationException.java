package no.idporten.eudiw.login.openid4vp;

import no.idporten.sdk.oidcserver.OAuth2Exception;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * Exception thrown when the verified credential is seen as invalid for some reason.
 */
public class InvalidVerificationException extends OAuth2Exception {

    private String logMessage;

    public InvalidVerificationException(String errorDescription) {
        this(errorDescription, null);
    }

    public InvalidVerificationException(String errorDescription, String logMessage) {
        super(OAuth2Exception.INVALID_REQUEST, errorDescription, HttpStatus.BAD_REQUEST.value());
        this.logMessage = logMessage;
    }

    public String getLogMessage() {
        return StringUtils.hasText(logMessage) ? logMessage : super.getMessage();
    }

}
