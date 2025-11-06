package no.idporten.eudiw.verifier.proxy;

import lombok.Getter;

@Getter
public class VerificationException extends RuntimeException {

    private String error;
    private String errorDescription;

    public VerificationException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.error = errorCode;
        this.errorDescription = errorMessage;
    }

}
