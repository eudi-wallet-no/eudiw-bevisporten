package no.idporten.eudiw.verifier;

public class StatusCommunicationException extends RuntimeException {

    private String errorCode;
    private String errorDescription;

    public StatusCommunicationException(String errorCode, String errorDescription, Exception exception) {
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
