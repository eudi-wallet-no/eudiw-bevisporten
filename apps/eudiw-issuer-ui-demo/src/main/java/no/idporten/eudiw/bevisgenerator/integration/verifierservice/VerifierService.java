package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationResult;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationStatus;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationTransactionData;

public interface VerifierService {
    VerificationTransactionData startVerification(String requestBody);
    VerificationResult retrieveVerificationResult(String transactionId);
    VerificationStatus retrieveVerificationStatus(String transactionId);
}
