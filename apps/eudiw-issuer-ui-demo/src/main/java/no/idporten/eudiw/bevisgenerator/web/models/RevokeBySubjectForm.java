package no.idporten.eudiw.bevisgenerator.web.models;

import jakarta.validation.constraints.NotBlank;

public record RevokeBySubjectForm(
        @NotBlank(message = "Credential configuration må velgast")
        String credentialConfigurationId,
        @NotBlank(message = "Subject må ha verdi")
        String subjectIdentifier
) {
    public RevokeBySubjectForm() {
        this("", "");
    }
}
