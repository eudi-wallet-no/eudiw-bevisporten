package no.idporten.eudiw.issuer.claimssource.pid;

public class FregIOException extends RuntimeException {

    private final String error;

    public FregIOException(String error, String errorDescription, Throwable cause) {
        super(errorDescription, cause);
        this.error = error;
    }
    public FregIOException(String error, String errorDescription) {
        super(errorDescription);
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return super.getMessage();
    }
}
