package no.idporten.eudiw.bevisgenerator.web.models;

import jakarta.validation.constraints.NotBlank;

public record RevokeForm(
        @NotBlank(message = "Vel ein bevistype")
        String credentialConfigurationId,
        @NotBlank(message = "Skriv inn transaksjons-ID-en")
        String issuanceTransactionId
) {
    public RevokeForm() {
        this("", "");
    }
}
