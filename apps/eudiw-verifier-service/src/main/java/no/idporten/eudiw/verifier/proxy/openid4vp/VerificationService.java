package no.idporten.eudiw.verifier.proxy.openid4vp;

import no.idporten.eudiw.verifier.proxy.VerificationException;
import no.idporten.eudiw.verifier.proxy.api.verification.StartVerificationRequest;
import no.idporten.eudiw.verifier.proxy.api.verification.StartVerificationResponse;
import no.idporten.eudiw.verifier.proxy.api.verification.VerificationResultResponse;
import no.idporten.eudiw.verifier.proxy.api.verification.VerificationStatusResponse;
import no.idporten.eudiw.verifier.proxy.config.VerifierProxyProperties;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.CredentialConfiguration;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.CredentialIssuerMetadata;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.VerifiedCredentials;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.UUID;

@Service
public class VerificationService {

    private final VerifierProxyProperties verifierProxyProperties;
    private final OpenID4VPRequestService openID4VPRequestService;
    private final VerificationTransactionService verificationTransactionService;

    public VerificationService(VerifierProxyProperties verifierProxyProperties, OpenID4VPRequestService openID4VPRequestService, VerificationTransactionService verificationTransactionService) {
        this.verifierProxyProperties = verifierProxyProperties;
        this.openID4VPRequestService = openID4VPRequestService;
        this.verificationTransactionService = verificationTransactionService;
    }

    public StartVerificationResponse startVerification(@RequestBody StartVerificationRequest startVerificationRequest) throws Exception {
        if (! verifierProxyProperties.getCredentialIssuers().contains(startVerificationRequest.credentialIssuer())) {
            throw new VerificationException("invalid_request", "Unsupported credential issuer");
        }
        CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.resolve(startVerificationRequest.credentialIssuer());
        CredentialConfiguration credentialConfiguration = credentialIssuerMetadata.findCredentialConfiguration(startVerificationRequest.credentialConfigurationId());
        if (credentialConfiguration == null) {
            throw new VerificationException("invalid_request", "Unknown credential configuration");
        }
        String verifierTransactionId = UUID.randomUUID().toString();
        URI requestUri = openID4VPRequestService.createAuthorizationRequest(credentialConfiguration, verifierTransactionId);
        verificationTransactionService.initTransaction(verifierTransactionId, credentialConfiguration);
        return new StartVerificationResponse(requestUri, verifierTransactionId);
    }

    public VerificationStatusResponse verifierStatus(String verifierTransactionId) {
        return new VerificationStatusResponse(
                verificationTransactionService.retrieveStatus(verifierTransactionId),
                verifierTransactionId);
    }

    public VerificationResultResponse retrieveVerificationData(String verifierTransactionId) {
        VerifiedCredentials verifiedCredentials = verificationTransactionService.retrieveVerifiedCredentials(verifierTransactionId);
        return new VerificationResultResponse(verifierTransactionId, verifiedCredentials.vpToken(), verifiedCredentials.credentials());
    }

}
