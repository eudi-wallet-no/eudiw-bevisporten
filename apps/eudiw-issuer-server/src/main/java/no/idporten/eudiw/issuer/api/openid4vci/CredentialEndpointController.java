package no.idporten.eudiw.issuer.api.openid4vci;

import com.nimbusds.jwt.JWT;
import io.swagger.v3.oas.annotations.Hidden;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.CredentialIssuerService;
import no.idporten.eudiw.issuer.openid4vci.nonce.NonceService;
import no.idporten.eudiw.issuer.openid4vci.proofs.InvalidProof;
import no.idporten.eudiw.issuer.openid4vci.proofs.ProofService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Hidden
@RestController
public class CredentialEndpointController {

    private static final String DPOP_HEADER = "DPoP";

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final CredentialIssuerService credentialIssuerService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final NonceService nonceService;
    private final ProofService proofService;

    public CredentialEndpointController(CredentialIssuerServerProperties credentialIssuerServerProperties, CredentialIssuerService credentialIssuerService, AuthorizationServerService authorizationServerService, AccessTokenValidationService accessTokenValidationService, NonceService nonceService, ProofService proofService) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.credentialIssuerService = credentialIssuerService;
        this.authorizationServerService = authorizationServerService;
        this.accessTokenValidationService = accessTokenValidationService;
        this.nonceService = nonceService;
        this.proofService = proofService;
    }

    @PostMapping(path = Endpoints.CREDENTIAL_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialResponse> credentialEndpoint(@RequestBody CredentialRequest credentialRequest,
                                                                 @RequestHeader(required = false, value = HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                                 @RequestHeader(required = false, value = DPOP_HEADER) String dpopHeader) {
        JWT accessToken = accessTokenValidationService.validateAccessTokenForCredentialConfiguration(
                HttpMethod.POST,
                Endpoints.endpointURI(credentialIssuerServerProperties.getCredentialIssuer(), Endpoints.CREDENTIAL_ENDPOINT),
                authorizationHeader,
                dpopHeader,
                List.of(authorizationServerService.getPrimaryAuthorizationServer()));
        credentialRequest.validate();
        if (credentialRequest.getProofs() == null && credentialRequest.getProof() == null) {
            throw new InvalidProof(nonceService.generateNonce(), "Credential Issuer requires key proof to be bound to a Credential Issuer provided nonce.");
        }
        if (credentialRequest.getProof() != null) {
            proofService.validateProof(credentialRequest.getProof());
        } if (credentialRequest.getProofs() != null) {
            proofService.validateProofs(credentialRequest.getProofs());
        }
        CredentialResponse credentialResponse = credentialIssuerService.issueCredentials(credentialRequest, accessToken);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(credentialResponse);
    }

}
