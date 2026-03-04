package no.idporten.eudiw.issuer.api.openid4vci;

import com.nimbusds.jwt.JWT;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.CredentialIssuerService;
import no.idporten.eudiw.issuer.openid4vci.nonce.NonceService;
import no.idporten.eudiw.issuer.openid4vci.proofs.InvalidProof;
import no.idporten.eudiw.issuer.openid4vci.proofs.ProofService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Hidden
@RestController
public class CredentialEndpointController {

    private final CredentialIssuerService credentialIssuerService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final NonceService nonceService;
    private final ProofService proofService;

    public CredentialEndpointController(CredentialIssuerService credentialIssuerService, AuthorizationServerService authorizationServerService, AccessTokenValidationService accessTokenValidationService, NonceService nonceService, ProofService proofService) {
        this.credentialIssuerService = credentialIssuerService;
        this.authorizationServerService = authorizationServerService;
        this.accessTokenValidationService = accessTokenValidationService;
        this.nonceService = nonceService;
        this.proofService = proofService;
    }

    @PostMapping(path = {Endpoints.CREDENTIAL_ENDPOINT, Endpoints.CREDENTIAL_ENDPOINT_TENANT}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialResponse> credentialEndpoint(
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @RequestBody CredentialRequest credentialRequest,
            HttpServletRequest request) {
        JWT accessToken = accessTokenValidationService.validateAccessToken(AccessTokenValidationContext.forDPoPToken(request, List.of(authorizationServerService.getPrimaryAuthorizationServer())));
        credentialRequest.validate();
        if (credentialRequest.getProofs() == null && credentialRequest.getProof() == null) {
            throw new InvalidProof(nonceService.generateNonce(), "Credential Issuer requires key proof to be bound to a Credential Issuer provided nonce.");
        }
        if (credentialRequest.getProof() != null) {
            proofService.validateProof(credentialRequest.getProof());
        }
        if (credentialRequest.getProofs() != null) {
            proofService.validateProofs(credentialRequest.getProofs());
        }
        CredentialResponse credentialResponse = credentialIssuerService.issueCredentials(credentialRequest, accessToken);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(credentialResponse);
    }

}
