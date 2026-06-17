package no.idporten.eudiw.verifier.proxy.openid4vp;

import no.idporten.eudiw.verifier.proxy.VerificationException;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.VerifiedCredentials;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class VerificationTransactionService {

    public static String STATUS_UNKNOWN = "UNKNOWN";
    public static String STATUS_WAIT = "WAIT";
    public static String STATUS_AVAILABLE = "AVAILABLE";

    private final Map<String, VerificationTransaction> verificationTransactions = new HashMap<>();

    public void initTransaction(Map<String, Object> dcqlQuery, String verifierTransactionId) {
        VerificationTransaction verificationTransaction = new VerificationTransaction();
        verificationTransaction.setDcqlQuery(dcqlQuery);
        verificationTransaction.setStatus(STATUS_WAIT);
        verificationTransactions.put(verifierTransactionId, verificationTransaction);
    }

    public VerificationTransaction getVerificationTransaction(String verifierTransactionId) {
        return verificationTransactions.get(verifierTransactionId);
    }

    public String retrieveStatus(String verifierTransactionId) {
        VerificationTransaction verificationTransaction = verificationTransactions.getOrDefault(verifierTransactionId, new VerificationTransaction());
        if (verificationTransaction.getStatus() == null) {
            verificationTransaction.setStatus(STATUS_UNKNOWN);
            verificationTransactions.put(verifierTransactionId, verificationTransaction);
        }
        return verificationTransaction.getStatus();
    }

    public void addVerifiedCredentials(String verifierTransactionId, VerifiedCredentials verifiedCredentials) {
        VerificationTransaction verificationTransaction = verificationTransactions.get(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        verificationTransaction.setStatus(STATUS_AVAILABLE);
        verificationTransaction.setVerifiedCredentials(verifiedCredentials);
        verificationTransactions.put(verifierTransactionId, verificationTransaction);
    }

    public VerifiedCredentials retrieveVerifiedCredentials(String verifierTransactionId) {
        VerificationTransaction verificationTransaction = verificationTransactions.get(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        if (verificationTransaction.getVerifiedCredentials() == null) {
            throw new VerificationException("invalid_request", "Verifier transaction data not available");
        }
        VerifiedCredentials verifiedCredentials = verificationTransaction.getVerifiedCredentials();
        verificationTransactions.remove(verifierTransactionId);
        return verifiedCredentials;
    }

}
