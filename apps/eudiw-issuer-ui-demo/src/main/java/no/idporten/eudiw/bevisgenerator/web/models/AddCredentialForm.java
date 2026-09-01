package no.idporten.eudiw.bevisgenerator.web.models;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import no.idporten.eudiw.bevisgenerator.web.models.unique.UniqueCredentialType;


public record AddCredentialForm(
        String id,

        @NotBlank(message = "Credential type er påkrevd")
        @Pattern(
                regexp = "^[a-z0-9_:.]{3,155}$",
                message = "Credential type kan bare inneholde små bokstaver, tall, kolon, punktum og understrek.\nLengde: 3–155 tegn"
        )
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
