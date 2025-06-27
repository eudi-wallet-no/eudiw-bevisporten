package no.idporten.eudiw.issuer.api;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialIssuerService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialResponse;
import no.idporten.eudiw.issuer.openid4vci.protocol.InvalidProof;
import no.idporten.eudiw.issuer.openid4vci.service.NonceService;
import no.idporten.eudiw.issuer.openid4vci.service.ProofService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class CredentialEndpointController {

    private final CredentialIssuerService credentialIssuerService;
    private final AuthorizationServerService authorizationServerService;
    private final NonceService nonceService;
    private final ProofService proofService;

    @PostMapping(path = Endpoints.CREDENTIAL_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialResponse> credentialEndpoint(@RequestBody CredentialRequest credentialRequest,
                                                                 @RequestHeader(required = false, value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        JWT accessToken = extractAccessTokenFromAuthorizationHeader(authorizationHeader);
        JWT validAccessToken = validateAccessToken(accessToken);
        credentialRequest.validate();
        if (credentialRequest.getProof() == null) {
            throw new InvalidProof(nonceService.generateNonce(), "Credential Issuer requires key proof to be bound to a Credential Issuer provided nonce.");
        }
        proofService.validateProof(credentialRequest.getProof());
        List<Credential> credentials = credentialIssuerService.issueCredentials(credentialRequest, validAccessToken);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(CredentialResponse.builder()
                        .credentials(credentials)
                        .build());
    }

    private JWT extractAccessTokenFromAuthorizationHeader(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new IssuerServerException("invalid_request", "Missing authorization header", HttpStatus.UNAUTHORIZED);
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new IssuerServerException("invalid_request", "Missing bearer token in authorization header", HttpStatus.UNAUTHORIZED);
        }
        String accessToken = authorizationHeader.substring("Bearer ".length());
        try {
            return SignedJWT.parse(accessToken);
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format", HttpStatus.UNAUTHORIZED);
        }
    }

    private JWT validateAccessToken(JWT accessToken) {
        try {
            AuthorizationServer authorizationServer = authorizationServerService.findAuthorizationServer(accessToken.getJWTClaimsSet().getIssuer());
            if (authorizationServer == null) {
                throw new IssuerServerException("invalid_token", "Unknown authorization server", HttpStatus.UNAUTHORIZED);
            }
            return authorizationServer.getAccessTokenValidator().validate(accessToken);
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format", HttpStatus.UNAUTHORIZED);
        }
    }

}
