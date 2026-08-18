package no.idporten.eudiw.verifier;

public class IOConnectionException extends RuntimeException {

    private String errorCode;
    private String errorDescription;

    public IOConnectionException(String errorCode, String errorDescription, Exception exception) {
        super(errorDescription, exception);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

}
