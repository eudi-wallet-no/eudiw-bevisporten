package no.idporten.eudiw.bevisgenerator.web.models;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import no.idporten.eudiw.bevisgenerator.web.models.unique.UniqueVct;


public record AddCredentialForm(
        String id,

        @NotBlank(message = "VCT er påkrevd")
        @Pattern(
                regexp = "^[a-z0-9_:]{3,155}",
                message = "VCT kan kun bestå av små bokstaver, tall, kolon og understrek.\n Lengde: 3-155 tegn"
        )
        @UniqueVct()
        String vct,
        String json
) {

    public AddCredentialForm(String json) {
        this("","", json);
    }

    public AddCredentialForm(String id, String vct, String json) {
        this.id = id;
        this.vct = vct;
        this.json = json;
    }
}
