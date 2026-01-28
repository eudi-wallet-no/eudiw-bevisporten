package no.idporten.eudiw.issuer.ui.demo.web.models.advancedForm;

import jakarta.validation.constraints.NotBlank;

public record ClaimForm(
        @NotBlank(message = "Path kan ikke være tom", groups = {CreateForm.class, EditForm.class})
        String path,
        @NotBlank(message = "Name kan ikke vær tom", groups = {CreateForm.class, EditForm.class})
        String name,

        String type,
        String mimeType,
        @NotBlank(message = "Eksempelverdi kan ikke være tom", groups = {CreateForm.class, EditForm.class})
        String exampleValue) {

    public ClaimForm(String path, String name, String exampleValue) {
        this(path, name, ClaimType.STRING, null, exampleValue);
    }

    public ClaimForm(String path, String name, String type, String exampleValue) {
        this(path, name, type, null, exampleValue);
    }
}
