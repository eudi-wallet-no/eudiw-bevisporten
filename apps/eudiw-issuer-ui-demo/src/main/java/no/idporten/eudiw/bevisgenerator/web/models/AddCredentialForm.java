package no.idporten.eudiw.bevisgenerator.web.models;


import jakarta.validation.constraints.NotBlank;
import no.idporten.eudiw.bevisgenerator.web.models.unique.UniqueCredentialType;


public record AddCredentialForm(
        String id,

        @NotBlank(message = "Credential type er påkrevd")
        @UniqueCredentialType()
        String credentialType,
        String json
) {

    public AddCredentialForm(String json) {
        this("","", json);
    }

    public AddCredentialForm(String id, String credentialType, String json) {
        this.id = id;
        this.credentialType = credentialType;
        this.json = json;
    }
}
