package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.cache.CacheService;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

@Service
public class VerificationTransactionService {

    public static String STATUS_UNKNOWN = "UNKNOWN";
    public static String STATUS_WAIT = "WAIT";
    public static String STATUS_AVAILABLE = "AVAILABLE";
    private final CacheService cacheService;

    public VerificationTransactionService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public void initTransaction(DcqlQuery dcqlQuery, URI redirectUri, String verifierTransactionId, ClientApplication clientApplication, boolean includeValidationDetails) {
        VerificationTransaction verificationTransaction = new VerificationTransaction();
        verificationTransaction.setDcqlQuery(dcqlQuery);
        verificationTransaction.setRedirectUri(redirectUri);
        verificationTransaction.setClientApplication(clientApplication);
        verificationTransaction.setStatus(STATUS_WAIT);
        verificationTransaction.setIncludeValidationDetails(includeValidationDetails);
        cacheService.putVerificationTransaction(clientApplication, verifierTransactionId, verificationTransaction);
    }

    public VerificationTransaction getVerificationTransaction(ClientApplication clientApplication, String verifierTransactionId) {
        return cacheService.getVerificationTransaction(clientApplication, verifierTransactionId);
    }

    public String retrieveStatus(ClientApplication clientApplication,String verifierTransactionId) {
        VerificationTransaction verificationTransaction = cacheService.getVerificationTransaction(clientApplication, verifierTransactionId);
        if (verificationTransaction.getStatus() == null) {
            verificationTransaction.setStatus(STATUS_UNKNOWN);
            cacheService.putVerificationTransaction(clientApplication, verifierTransactionId, verificationTransaction);
        }
        if(!Objects.equals(clientApplication.getKeystoreName(), verificationTransaction.getClientApplication().getKeystoreName())) {
            throw new VerificationException("invalid_request", "client application id does not match transactions client application id");
        }
        return verificationTransaction.getStatus();
    }

    public void addVerifiedCredentials(ClientApplication clientApplication, String verifierTransactionId, VerifiedCredentials verifiedCredentials, Map<String, Object> vpTokenResponse) {
        VerificationTransaction verificationTransaction = cacheService.getVerificationTransaction(clientApplication, verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        verificationTransaction.setStatus(STATUS_AVAILABLE);
        verificationTransaction.setVerifiedCredentials(verifiedCredentials);
        verificationTransaction.setResponse(vpTokenResponse);
        cacheService.updateVerificationTransaction(clientApplication, verifierTransactionId, verificationTransaction);
    }

    public VerificationTransaction retrieveVerifiedCredentials(ClientApplication clientApplication, String verifierTransactionId) {
        if(clientApplication == null) {
            throw new VerificationException("invalid_request", "Unknown client application");
        }
        VerificationTransaction verificationTransaction = cacheService.getVerificationTransaction(clientApplication, verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        if (verificationTransaction.getVerifiedCredentials() == null) {
            throw new VerificationException("invalid_request", "Verifier transaction data not available");
        }
        if(!Objects.equals(clientApplication.getKeystoreName(), verificationTransaction.getClientApplication().getKeystoreName())) {
            throw new VerificationException("invalid_request", "client application name does not match transactions client application name");
        }
        cacheService.removeVerificationTransaction(clientApplication, verifierTransactionId);
        return verificationTransaction;
    }

}
