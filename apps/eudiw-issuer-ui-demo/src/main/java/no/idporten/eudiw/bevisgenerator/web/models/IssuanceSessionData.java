package no.idporten.eudiw.bevisgenerator.web.models;

public record IssuanceSessionData(
        String credentialIssuer,
        String credentialConfigurationId,
        String credentialName,
        String subjectIdentifier,
        boolean completed
) {

    public IssuanceSessionData toCompleted() {
        return new IssuanceSessionData(
                credentialIssuer,
                credentialConfigurationId,
                credentialName,
                subjectIdentifier,
                true
        );
    }
}
