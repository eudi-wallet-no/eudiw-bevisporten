package no.idporten.eudiw.bevisgenerator.web.models;

import jakarta.validation.constraints.NotBlank;

public record RevokeForm(
        @NotBlank(message = "Credential configuration må velgast")
        String credentialConfigurationId,
        @NotBlank(message = "Issuance transaction id må ha verdi")
        String issuanceTransactionId
) {
    public RevokeForm() {
        this("", "");
    }
}
