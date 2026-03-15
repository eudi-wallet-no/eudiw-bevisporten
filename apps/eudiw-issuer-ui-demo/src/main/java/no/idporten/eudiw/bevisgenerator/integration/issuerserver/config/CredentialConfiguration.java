package no.idporten.eudiw.bevisgenerator.integration.issuerserver.config;

public record CredentialConfiguration(String credentialIssuer, String credentialConfigurationId, String scope, String personIdentifier, String description, String jsonRequest) { }
