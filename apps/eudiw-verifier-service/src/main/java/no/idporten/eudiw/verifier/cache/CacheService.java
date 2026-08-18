package no.idporten.eudiw.verifier.cache;

import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.VerificationTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class CacheService {

    @Value("${spring.application.name}")
    private String applicationName;

    private final Cache cache;

    public CacheService(Cache cache) {
        this.cache = cache;
    }

    protected String verificationTransactionKey(ClientApplication clientApplication, String verifierTransactionId) {
        return applicationName + ":" + clientApplication.getId() + ":verification-transactions:" + verifierTransactionId;
    }


    public void putVerificationTransaction(ClientApplication clientApplication, String verificationTransactionId, VerificationTransaction verificationTransaction) {
        if (getVerificationTransaction(clientApplication,  verificationTransactionId) == null) {
            cache.set(verificationTransactionKey(clientApplication, verificationTransactionId), verificationTransaction);
        }
    }

    public void updateVerificationTransaction(ClientApplication clientApplication, String verificationTransactionId, VerificationTransaction verificationTransaction) {
        cache.set(verificationTransactionKey(clientApplication, verificationTransactionId), verificationTransaction);

    }

    public VerificationTransaction removeVerificationTransaction(ClientApplication clientApplication, String verificationTransactionId) {
        return (VerificationTransaction) cache.remove(verificationTransactionKey(clientApplication, verificationTransactionId));
    }
    public VerificationTransaction getVerificationTransaction(ClientApplication clientApplication, String verificationTransactionId) {
        return (VerificationTransaction) cache.get(verificationTransactionKey(clientApplication, verificationTransactionId));
    }


    protected String authorizationRequestKey(ClientApplication clientApplication, String requestId) {
        return applicationName + ":" + clientApplication.getId() + ":authorization-requests:" + requestId;
    }

    public void putAuthorizationRequest(ClientApplication clientApplication, String requestId, String verificationTransactionId) {
        cache.set(authorizationRequestKey(clientApplication, requestId), verificationTransactionId);
    }

    public String retrieveAuthorizationRequest(ClientApplication clientApplication, String requestId) {
            return (String) cache.remove(authorizationRequestKey(clientApplication, requestId));
        }
}
