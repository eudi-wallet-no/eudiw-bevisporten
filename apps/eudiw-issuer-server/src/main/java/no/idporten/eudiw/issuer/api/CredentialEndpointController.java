package no.idporten.eudiw.issuer.api;

import com.nimbusds.jwt.JWT;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.claimssource.CredentialIssuerService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialResponse;
import no.idporten.eudiw.issuer.openid4vci.protocol.InvalidProof;
import no.idporten.eudiw.issuer.openid4vci.service.NonceService;
import no.idporten.eudiw.issuer.openid4vci.service.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.openid4vci.service.ProofService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Hidden
@RequiredArgsConstructor
@RestController
public class CredentialEndpointController {

    private final CredentialIssuerService credentialIssuerService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final NonceService nonceService;
    private final ProofService proofService;
    private final CredentialIssuanceStatusService credentialIssuanceStatusService;

    @PostMapping(path = Endpoints.CREDENTIAL_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialResponse> credentialEndpoint(@RequestBody CredentialRequest credentialRequest,
                                                                 @RequestHeader(required = false, value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        JWT accessToken = accessTokenValidationService.validateAccessTokenForCredentialConfiguration(authorizationHeader, List.of(authorizationServerService.getPrimaryAuthorizationServer()));
        credentialRequest.validate();
        if (credentialRequest.getProof() == null) {
            throw new InvalidProof(nonceService.generateNonce(), "Credential Issuer requires key proof to be bound to a Credential Issuer provided nonce.");
        }
        proofService.validateProof(credentialRequest.getProof());
        CredentialResponse  credentialResponse = credentialIssuerService.issueCredentials(credentialRequest, accessToken);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(credentialResponse);
    }

}
