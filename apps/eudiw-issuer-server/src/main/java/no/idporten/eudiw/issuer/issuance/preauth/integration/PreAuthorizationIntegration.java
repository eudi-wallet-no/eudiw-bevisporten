package no.idporten.eudiw.issuer.issuance.preauth.integration;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.config.CredentialConfigurationService;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.issuance.preauth.PreAuthorizedIssuanceRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service adding pre-authorizations to auth proxy.
 */
@RequiredArgsConstructor
@Service
public class PreAuthorizationIntegration {

    public static final String PRE_AUTHORIZATIONS_ENDPOINT = "/api/v1/pre-authorizations";

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final CredentialConfigurationService credentialConfigurationService;
    private final RestClient preAuthorizationRestClient;

    public String preAuthorize(IssuanceTransactionId issuanceTransactionId, PreAuthorizedIssuanceRequest preAuthorizedIssuanceRequest) {
        ExtendedCredentialConfiguration credentialConfiguration = credentialConfigurationService.findCredentialConfiguration(preAuthorizedIssuanceRequest.getCredentialConfigurationId());
        PreAuthorizationRequest preAuthorizationRequest = PreAuthorizationRequest.builder()
                .aud(credentialIssuerServerProperties.getCredentialIssuer().toString())
                .sub(preAuthorizedIssuanceRequest.getSubject().getIdentifier())
                .scope(credentialConfiguration.getScope())
                .txId(issuanceTransactionId.getValue())
                // .txCodeChallenge("A6xnQhbz4Vx2HuGl4lXwZ5U2I8iziLRFnhP5eNfIRvQ") // TODO tx_code senere feature
                .authorizationLifetimeSeconds(credentialConfiguration.getCredentialIssuerContext().getPreAuthorizationLifetime().toSeconds())
                .build();
        PreAuthorizationResponse preAuthorizationResponse = preAuthorizationRestClient
                .post()
                .uri(PRE_AUTHORIZATIONS_ENDPOINT)
                .body(preAuthorizationRequest)
                .retrieve()
                .body(PreAuthorizationResponse.class);
        return preAuthorizationResponse.getPreAuthorizedCode();
    }

}
