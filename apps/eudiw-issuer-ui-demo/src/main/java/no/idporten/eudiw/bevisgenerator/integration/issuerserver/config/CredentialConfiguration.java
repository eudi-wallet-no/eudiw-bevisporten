package no.idporten.eudiw.bevisgenerator.integration.issuerserver.config;

public record CredentialConfiguration(
        String credentialIssuer,
        String credentialConfigurationId,
        String scope,
        String personIdentifier,
        String description,
        String jsonRequest,
        String localId) {

    public CredentialConfiguration(
            String credentialIssuer,
            String credentialConfigurationId,
            String scope,
            String personIdentifier,
            String description,
            String jsonRequest
    ) {
        this(
                credentialIssuer,
                credentialConfigurationId,
                scope,
                personIdentifier,
                description,
                jsonRequest,
                credentialConfigurationId
        );
    }

    public String selectionId() {
        return localId == null || localId.isBlank() ? credentialConfigurationId : localId;
    }
}
