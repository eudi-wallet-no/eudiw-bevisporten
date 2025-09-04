package no.idporten.eudiw.issuer.openid4vci.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.oauth2.PreAuthorizationRequest;
import no.idporten.eudiw.issuer.oauth2.PreAuthorizationResponse;
import no.idporten.eudiw.issuer.openid4vci.protocol.StartIssuanceRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service adding pre-authorizations to auth proxy.
 */
@RequiredArgsConstructor
@Service
public class PreAuthorizationService {

    public static final String PRE_AUTHORIZATIONS_ENDPOINT = "/api/v1/pre-authorizations";

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final RestClient preAuthorizationRestClient;

    public IssuerTransactionId generateIssuerTransactionCode() {
        return new IssuerTransactionId();
    }

    public String preAuthorize(IssuerTransactionId issuerTransactionCode, StartIssuanceRequest startIssuanceRequest) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(startIssuanceRequest.getCredentialConfigurationId());
        PreAuthorizationRequest preAuthorizationRequest = PreAuthorizationRequest.builder()
                .aud(credentialIssuerServerProperties.getCredentialIssuer().toString())
                .sub("12345678901") // TODO egen sak på å finne hva som må i access_token
                .scope(credentialConfigurationProperties.getScope())
                .txId(issuerTransactionCode.getValue())
                // .txCodeChallenge("A6xnQhbz4Vx2HuGl4lXwZ5U2I8iziLRFnhP5eNfIRvQ") // TODO tx_code senere feature
                .authorizationLifetimeSeconds(600)
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
