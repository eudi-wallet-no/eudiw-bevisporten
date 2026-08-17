package no.idporten.eudiw.connector.authoritativesources.exceptions;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources;

import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.NOT_FOUND_CREDENTIAL_DATA;
import static org.springframework.http.HttpStatus.NOT_FOUND;

public class AuthoritativeSourceDataNotFoundException extends AuthoritativeSourceException {

    public AuthoritativeSourceDataNotFoundException(AuthoritativeSources authoritativeSource, String message, String logMessage) {
        super(authoritativeSource, NOT_FOUND_CREDENTIAL_DATA, message, NOT_FOUND, logMessage);
    }

}
