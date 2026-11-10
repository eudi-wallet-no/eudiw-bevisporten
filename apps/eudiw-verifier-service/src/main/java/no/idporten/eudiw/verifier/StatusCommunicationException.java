package no.idporten.eudiw.verifier;

public class StatusCommunicationException extends VerificationException {

    private ErrorCode errorCode;
    private String errorDescription;
    private String logMessage;

    public StatusCommunicationException(ErrorCode errorCode, String errorDescription, String logMessage, Exception exception) {
        super(errorCode, errorDescription, logMessage, exception);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.logMessage = logMessage;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

}
