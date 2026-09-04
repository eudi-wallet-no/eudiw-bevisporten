package no.idporten.eudiw.bevisgenerator.web.models;


public record CredentialDto(String credentialType, String name, String json) {
    public CredentialDto {
        name = name == null ? cleanCredentialType(credentialType) : name;
    }

    public CredentialDto(String credentialType, String json) {
        this(credentialType, null, json);
    }

    private static String cleanCredentialType(String credentialType) {
        return credentialType.replaceFirst("^[^:]+:", "");
    }
}
