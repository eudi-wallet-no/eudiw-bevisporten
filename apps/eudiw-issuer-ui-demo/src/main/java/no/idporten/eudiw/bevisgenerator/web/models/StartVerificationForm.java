package no.idporten.eudiw.bevisgenerator.web.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StartVerificationForm(
        @NotBlank(message = "Credential configuration må velgast")
        String credentialConfigurationId,
        @NotEmpty(message = "Minst eitt claim må velgast")
        List<@NotBlank(message = "Minst eitt claim må velgast") String> selectedClaimPaths
) {
    public StartVerificationForm() {
        this("", List.of());
    }
}
