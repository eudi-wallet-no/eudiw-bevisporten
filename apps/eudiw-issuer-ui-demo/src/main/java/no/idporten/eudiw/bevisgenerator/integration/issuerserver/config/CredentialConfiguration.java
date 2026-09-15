package no.idporten.eudiw.bevisgenerator.integration.issuerserver.config;

import org.springframework.boot.context.properties.bind.ConstructorBinding;

public record CredentialConfiguration(
        String credentialIssuer,
        String credentialConfigurationId,
        String scope,
        String personIdentifier,
        String description,
        String jsonRequest,
        String localId) {

    @ConstructorBinding
    public CredentialConfiguration {
    }

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
