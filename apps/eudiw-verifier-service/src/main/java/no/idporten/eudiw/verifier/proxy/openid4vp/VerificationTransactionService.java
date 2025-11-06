package no.idporten.eudiw.verifier.proxy.openid4vp;

import no.idporten.eudiw.verifier.proxy.VerificationException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class VerificationTransactionService {

    private final Map<String, VerificationTransaction> verificationTransactions = new HashMap<>();

    public void updateStatus(String verifierTransactionId, String status) {
        VerificationTransaction verificationTransaction = verificationTransactions.getOrDefault(verifierTransactionId, new VerificationTransaction());
        verificationTransaction.setStatus(status);
        verificationTransactions.put(verifierTransactionId, verificationTransaction);
    }

    public String retrieveStatus(String verifierTransactionId, String defaultStatus) {
        VerificationTransaction verificationTransaction = verificationTransactions.getOrDefault(verifierTransactionId, new VerificationTransaction());
        if (verificationTransaction.getStatus() == null) {
            verificationTransaction.setStatus(defaultStatus);
            verificationTransactions.put(verifierTransactionId, verificationTransaction);
        }
        return verificationTransaction.getStatus();
    }

    public void addVerifiedCredentials(String verifierTransactionId, Map<String, Object> verifiedCredentials) {
        VerificationTransaction verificationTransaction = verificationTransactions.get(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        verificationTransaction.setStatus("AVAILABLE");
        verificationTransaction.setVerifiedCredentials(verifiedCredentials);
        verificationTransactions.put(verifierTransactionId, verificationTransaction);
    }

    public Map<String, Object> retrieveVerifiedCredentials(String verifierTransactionId) {
        VerificationTransaction verificationTransaction = verificationTransactions.get(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        if (verificationTransaction.getVerifiedCredentials() == null) {
            throw new VerificationException("invalid_request", "Verifier transaction data not available");
        }
        Map<String, Object> verifiedCredentials = verificationTransaction.getVerifiedCredentials();
        verificationTransactions.remove(verifierTransactionId);
        return verifiedCredentials;
    }

}
