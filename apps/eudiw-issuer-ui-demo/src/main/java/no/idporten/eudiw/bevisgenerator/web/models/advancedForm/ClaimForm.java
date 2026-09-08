package no.idporten.eudiw.bevisgenerator.web.models.advancedForm;

import jakarta.validation.constraints.NotBlank;

public record ClaimForm(
        @NotBlank(message = "Path kan ikkje vere tom", groups = {CreateForm.class, EditForm.class})
        String path,
        @NotBlank(message = "Namn kan ikkje vere tomt", groups = {CreateForm.class, EditForm.class})
        String name,

        String type,
        String mimeType,
        @NotBlank(message = "Dømeverdi kan ikkje vere tom", groups = {CreateForm.class, EditForm.class})
        String exampleValue) {

    public ClaimForm(String path, String name, String exampleValue) {
        this(path, name, ClaimType.STRING, null, exampleValue);
    }

    public ClaimForm(String path, String name, String type, String exampleValue) {
        this(path, name, type, null, exampleValue);
    }
}
