package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import no.idporten.eudiw.verifier.config.ClientApplication;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class VerificationTransactionService {

    public static String STATUS_UNKNOWN = "UNKNOWN";
    public static String STATUS_WAIT = "WAIT";
    public static String STATUS_AVAILABLE = "AVAILABLE";

    private final Map<String, VerificationTransaction> verificationTransactions = new HashMap<>();

    public void initTransaction(DcqlQuery dcqlQuery, URI redirectUri, String verifierTransactionId, ClientApplication clientApplication) {
        VerificationTransaction verificationTransaction = new VerificationTransaction();
        verificationTransaction.setDcqlQuery(dcqlQuery);
        verificationTransaction.setRedirectUri(redirectUri);
        verificationTransaction.setClientApplication(clientApplication);
        verificationTransaction.setStatus(STATUS_WAIT);
        verificationTransactions.put(verifierTransactionId, verificationTransaction);
    }

    public VerificationTransaction getVerificationTransaction(String verifierTransactionId) {
        return verificationTransactions.get(verifierTransactionId);
    }

    public String retrieveStatus(ClientApplication clientApplication,String verifierTransactionId) {
        VerificationTransaction verificationTransaction = verificationTransactions.getOrDefault(verifierTransactionId, new VerificationTransaction());
        if (verificationTransaction.getStatus() == null) {
            verificationTransaction.setStatus(STATUS_UNKNOWN);
            verificationTransactions.put(verifierTransactionId, verificationTransaction);
        }
        if(!Objects.equals(clientApplication.getKeystoreName(), verificationTransaction.getClientApplication().getKeystoreName())) {
            throw new VerificationException("invalid_request", "client application id does not match transactions client application id");
        }
        return verificationTransaction.getStatus();
    }

    public void addVerifiedCredentials(String verifierTransactionId, VerifiedCredentials verifiedCredentials) {
        VerificationTransaction verificationTransaction = verificationTransactions.get(verifierTransactionId); // is "final result" of cacheKey already
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        verificationTransaction.setStatus(STATUS_AVAILABLE);
        verificationTransaction.setVerifiedCredentials(verifiedCredentials);
        verificationTransactions.put(verifierTransactionId, verificationTransaction);
    }

    public VerifiedCredentials retrieveVerifiedCredentials(ClientApplication clientApplication, String verifierTransactionId) {
        if(clientApplication == null) {
            throw new VerificationException("invalid_request", "Unknown client application");
        }
        VerificationTransaction verificationTransaction = verificationTransactions.get(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        if (verificationTransaction.getVerifiedCredentials() == null) {
            throw new VerificationException("invalid_request", "Verifier transaction data not available");
        }
        if(!Objects.equals(clientApplication.getKeystoreName(), verificationTransaction.getClientApplication().getKeystoreName())) {
            throw new VerificationException("invalid_request", "client application name does not match transactions client application name");
        }
        VerifiedCredentials verifiedCredentials = verificationTransaction.getVerifiedCredentials();
        verificationTransactions.remove(verifierTransactionId);
        return verifiedCredentials;
    }

}
