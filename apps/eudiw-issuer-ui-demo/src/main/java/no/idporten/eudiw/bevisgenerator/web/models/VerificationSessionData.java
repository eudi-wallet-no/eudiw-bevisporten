package no.idporten.eudiw.bevisgenerator.web.models;

import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationTransactionData;

public record VerificationSessionData(
        VerificationTransactionData transactionData,
        String credentialName
) {
}
