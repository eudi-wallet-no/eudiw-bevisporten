package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.dpop.JWKThumbprintConfirmation;
import com.nimbusds.oauth2.sdk.dpop.verifiers.*;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class AccessTokenValidationService {

    private final AuthorizationServerService authorizationServerService;

    /**
     * Validate that access_token is valid for our service and issued by a trusted authorization server.
     */
    public JWT validateAccessTokenForCredentialConfiguration(String authorizationHeader, List<AuthorizationServer> authorizationServers) {
        return validateAccessTokenForCredentialConfiguration(null, null, authorizationHeader, null, authorizationServers);
    }
    /**
     * Validates that access_token that may be DPoP bound is valid for our service and issued by a trusted authorization server.
     * Ignores DPoP if Bearer token or method or endpointURI is not specified.
     */
    public JWT validateAccessTokenForCredentialConfiguration(HttpMethod method, URI endpointUri, String authorizationHeader, String dPoPHeader, List<AuthorizationServer> authorizationServers) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new IssuerServerException("invalid_request", "Missing authorization header.", HttpStatus.UNAUTHORIZED);
        }
        try {
            AccessToken accessToken = AccessToken.parse(authorizationHeader, AccessTokenType.DPOP);
            JWT jwtAccessToken = SignedJWT.parse(accessToken.getValue());
            AuthorizationServer authorizationServer = authorizationServerService.findAuthorizationServer(jwtAccessToken.getJWTClaimsSet().getIssuer(), authorizationServers);
            if (authorizationServer == null) {
                throw new IssuerServerException("invalid_token", "Unknown authorization server.", HttpStatus.UNAUTHORIZED);
            }
            JWT validAccessToken = authorizationServer.getAccessTokenValidator().validate(jwtAccessToken);
            if (AccessTokenType.DPOP.equals(accessToken.getType())) {
                validateDPopAccessToken(method, endpointUri, authorizationHeader, dPoPHeader);
            }
            return validAccessToken;

        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format.", HttpStatus.UNAUTHORIZED);
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid authentication scheme.", HttpStatus.UNAUTHORIZED);
        }
    }

    protected void validateDPopAccessToken(HttpMethod method, URI endpointUri, String authorizationHeader, String dPoPHeader) {
        try {
            if (method == null || endpointUri == null) {
                return; // endpoint does not require DPoP
            }
            if (! StringUtils.hasText(dPoPHeader)) {
                throw new IssuerServerException("invalid_request", "Missing DPoP header.", HttpStatus.UNAUTHORIZED);
            }
            DPoPAccessToken dPoPAccessToken = DPoPAccessToken.parse(authorizationHeader);
            JWT jwtAccessToken = SignedJWT.parse(dPoPAccessToken.getValue());
            String clientIdClaim = jwtAccessToken.getJWTClaimsSet().getStringClaim("client_id");
            ClientID clientID = new ClientID(StringUtils.hasText(clientIdClaim) ? clientIdClaim : "unknown-wallet");
            SignedJWT dPopProof = SignedJWT.parse(dPoPHeader);
            JWKThumbprintConfirmation cnf = JWKThumbprintConfirmation.parse(jwtAccessToken.getJWTClaimsSet());
            DPoPProtectedResourceRequestVerifier requestVerifier = new DPoPProtectedResourceRequestVerifier(
                    Set.of(JWSAlgorithm.ES256),
                    10,
                    120,
                    new InMemoryDPoPSingleUseChecker(120, 240));
            DPoPIssuer dpopIssuer = new DPoPIssuer(clientID);
            requestVerifier.verify(method.name(),
                    endpointUri,
                    dpopIssuer,
                    dPopProof,
                    dPoPAccessToken,
                    cnf,
                    null // TODO nonce
            );
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            throw new IssuerServerException("invalid_token", "Failed to parse DPoP access token", HttpStatus.UNAUTHORIZED, e);
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_dpop_proof", "Failed to parse DPoP proof", HttpStatus.UNAUTHORIZED, e);
        } catch (InvalidDPoPProofException e) {
            throw new IssuerServerException("invalid_dpop_proof", "Invalid DPoP proof", HttpStatus.UNAUTHORIZED, e);
        } catch (AccessTokenValidationException e) {
            throw new IssuerServerException("invalid_token", "Access token validation failed", HttpStatus.UNAUTHORIZED, e);
        } catch (JOSEException e) {
            throw new IssuerServerException("invalid_dpop_proof", "Failed to validate DPoP proof", HttpStatus.UNAUTHORIZED, e);
        }
    }

    /**
     * Checks that a validated access token also meets the requirements of the credential configuration.
     */
    public void validateAccessTokenForCredentialConfiguration(JWT accessToken, String requiredAuthorizationServer, String requiredScope) {
        try {
            if (! requiredAuthorizationServer.equals(accessToken.getJWTClaimsSet().getIssuer())) {
                throw new IssuerServerException("invalid_token", "Invalid authorization server for credential configuration.", HttpStatus.UNAUTHORIZED);
            }
            if (! validateScope(requiredScope, accessToken)) {
                throw new IssuerServerException("insufficient_scope", "Invalid scope for credential configuration.", HttpStatus.FORBIDDEN);
            }

        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format.", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Checks that a validated access token is bound to subject.
     */
    public void validateAccessTokenBoundToSubject(JWT accessToken, String personIdentifier) {
        try {
            String pid = accessToken.getJWTClaimsSet().getStringClaim("pid");
            if (! StringUtils.hasText(pid)) {
                throw new IssuerServerException("invalid_token", "Token must contain person identifier in pid claim.", HttpStatus.UNAUTHORIZED);
            }
            if (! Objects.equals(pid, personIdentifier)) {
                throw new IssuerServerException("invalid_token", "Token and request subject/person identifier does not match.", HttpStatus.UNAUTHORIZED);
            }
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format.", HttpStatus.UNAUTHORIZED);
        }
    }

    protected boolean validateScope(String requiredScope, JWT accessToken) {
        try {
            String scope = accessToken.getJWTClaimsSet().getStringClaim("scope");
            if (! StringUtils.hasText(scope)) {
                return false;
            }
            List<String> parsedScopes = List.of(scope.split("\\s+"));
            return parsedScopes.contains(requiredScope);
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format.", HttpStatus.UNAUTHORIZED);
        }
    }

}
