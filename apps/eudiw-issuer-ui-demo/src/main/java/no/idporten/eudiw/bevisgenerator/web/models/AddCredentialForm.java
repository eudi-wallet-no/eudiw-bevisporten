package no.idporten.eudiw.bevisgenerator.web.models;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import no.idporten.eudiw.bevisgenerator.web.models.unique.UniqueCredentialType;


public record AddCredentialForm(
        String id,

        @NotBlank(message = "ID (credentialType) er påkravd")
        @Pattern(
                regexp = "^[a-z0-9_:.]{3,155}$",
                message = "ID (credentialType) kan berre innehalde små bokstavar, tal, kolon, punktum og understrek. Lengd: 3–155 teikn"
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
