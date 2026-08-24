package no.idporten.eudiw.bevisgenerator.web.models;

import jakarta.validation.constraints.NotBlank;

public record RevokeBySubjectForm(
        @NotBlank(message = "Vel ein bevistype")
        String credentialConfigurationId,
        @NotBlank(message = "Skriv inn personidentifikatoren")
        String subjectIdentifier
) {
    public RevokeBySubjectForm() {
        this("", "");
    }
}
