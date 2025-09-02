package no.idporten.eudiw.issuer.api;

import com.nimbusds.jwt.JWT;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.claimssource.CredentialIssuerService;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import no.idporten.eudiw.issuer.openid4vci.protocol.StartIssuanceRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.StartIssuanceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class StartCredentialIssuanceController {

    private final CredentialIssuerService credentialIssuerService;
    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    @PostMapping(path = Endpoints.START_CREDENTIAL_ISSUANCE_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StartIssuanceResponse> startIssuanceEndpoint(@RequestBody StartIssuanceRequest startIssuanceRequest) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(startIssuanceRequest.getCredentialConfigurationId());
        // TODO access_token egen sak
        JWT accessTpken = null;
        CredentialOffer credentialOffer = credentialIssuerService.startIssuerTransaction(startIssuanceRequest, accessTpken);
        StartIssuanceResponse response = StartIssuanceResponse.builder()
                .credentialOffer(credentialOffer)
                .build();
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

}
