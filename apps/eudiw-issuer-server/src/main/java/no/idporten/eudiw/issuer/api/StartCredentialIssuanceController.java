package no.idporten.eudiw.issuer.api;

import com.nimbusds.jwt.JWT;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.claimssource.CredentialIssuerService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import no.idporten.eudiw.issuer.openid4vci.protocol.StartIssuanceRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.StartIssuanceResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class StartCredentialIssuanceController {

    private final CredentialIssuerService credentialIssuerService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;

    @PostMapping(path = Endpoints.START_CREDENTIAL_ISSUANCE_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StartIssuanceResponse> startIssuanceEndpoint(
            @RequestBody StartIssuanceRequest startIssuanceRequest,
            @RequestHeader(required = false, value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        JWT accessToken = accessTokenValidationService.validateAccessTokenForCredentialConfiguration(authorizationHeader, authorizationServerService.getPreAuthorizationServers());
        CredentialOffer credentialOffer = credentialIssuerService.startIssuerTransaction(startIssuanceRequest, accessToken);
        StartIssuanceResponse response = StartIssuanceResponse.builder()
                .credentialOffer(credentialOffer)
                .build();
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

}
