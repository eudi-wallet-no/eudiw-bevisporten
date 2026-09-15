package no.idporten.eudiw.bevisgenerator.web.models;

import java.io.Serializable;
import java.util.List;

public record IssuanceSessionData(
        String credentialIssuer,
        String credentialConfigurationId,
        String credentialName,
        String subjectIdentifier,
        boolean completed,
        List<ClaimView> claims,
        String credentialConfigurationSelectionId
) implements Serializable {

    public IssuanceSessionData(
            String credentialIssuer,
            String credentialConfigurationId,
            String credentialName,
            String subjectIdentifier,
            boolean completed
    ) {
        this(
                credentialIssuer,
                credentialConfigurationId,
                credentialName,
                subjectIdentifier,
                completed,
                List.of(),
                credentialConfigurationId
        );
    }

    public IssuanceSessionData(
            String credentialIssuer,
            String credentialConfigurationId,
            String credentialName,
            String subjectIdentifier,
            boolean completed,
            List<ClaimView> claims
    ) {
        this(
                credentialIssuer,
                credentialConfigurationId,
                credentialName,
                subjectIdentifier,
                completed,
                claims,
                credentialConfigurationId
        );
    }

    public IssuanceSessionData toCompleted() {
        return new IssuanceSessionData(
                credentialIssuer,
                credentialConfigurationId,
                credentialName,
                subjectIdentifier,
                true,
                claims,
                credentialConfigurationSelectionId
        );
    }
}
