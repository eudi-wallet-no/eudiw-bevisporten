package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jose.JOSEException;
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
import no.idporten.eudiw.issuer.claimssource.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class AccessTokenValidationService {

    private final AuthorizationServerService authorizationServerService;

    /**
     * Validate access_token.
     *
     * @param context validation context with headers and request info
     * @return validated access_token JWT
     */
    public JWT validateAccessToken(AccessTokenValidationContext context) {
        if (!StringUtils.hasText(context.authorizationHeader())) {
            throw new IssuerServerException(ErrorCode.UNAUTHORIZED_INVALID_REQUEST, "Missing authorization header.");
        }
        try {
            AccessToken accessToken = AccessToken.parse(context.authorizationHeader(), AccessTokenType.DPOP);
            JWT jwtAccessToken = SignedJWT.parse(accessToken.getValue());
            AuthorizationServer authorizationServer = authorizationServerService.findAuthorizationServerByIssuer(jwtAccessToken.getJWTClaimsSet().getIssuer(), context.authorizationServers());
            if (authorizationServer == null) {
                throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Unknown authorization server.");
            }
            JWT validAccessToken = authorizationServer.getAccessTokenValidator().validate(jwtAccessToken, context.audience());
            if (AccessTokenType.DPOP.equals(accessToken.getType())) {
                validateDPopAccessToken(authorizationServer, context);
            }
            return validAccessToken;

        } catch (ParseException e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Invalid token format.");
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Invalid authentication scheme.");
        }
    }

    /**
     * Additional validation of DPoP when provided in request or required by endpoint.
     */
    protected void validateDPopAccessToken(AuthorizationServer authorizationServer, AccessTokenValidationContext context) {
        try {
            if ((context.endpointHttpMethod() == null || context.endpointURI() == null || !StringUtils.hasText(context.dpopHeader())) && !context.dPoPRequired()) {
                return;
            }
            if (context.dPoPRequired() && !StringUtils.hasText(context.dpopHeader())) {
                throw new IssuerServerException(ErrorCode.UNAUTHORIZED_INVALID_REQUEST, "Missing DPoP header.");
            }
            if (context.dPoPRequired() && (context.endpointHttpMethod() == null || context.endpointURI() == null)) {
                throw new IssuerServerException(ErrorCode.UNAUTHORIZED_INVALID_REQUEST, "Missing DPoP configuration");
            }
            DPoPAccessToken dPoPAccessToken = DPoPAccessToken.parse(context.authorizationHeader());
            JWT jwtAccessToken = SignedJWT.parse(dPoPAccessToken.getValue());
            String clientIdClaim = jwtAccessToken.getJWTClaimsSet().getStringClaim("client_id");
            ClientID clientID = new ClientID(StringUtils.hasText(clientIdClaim) ? clientIdClaim : "unknown-wallet");
            SignedJWT dPopProof = SignedJWT.parse(context.dpopHeader());
            JWKThumbprintConfirmation cnf = JWKThumbprintConfirmation.parse(jwtAccessToken.getJWTClaimsSet());
            DPoPProtectedResourceRequestVerifier requestVerifier = new DPoPProtectedResourceRequestVerifier(
                    authorizationServer.getDPoPAlgorithms(),
                    authorizationServer.getDPoPTimeSkewSeconds(),
                    authorizationServer.getDPoPMaxAgeSeconds(),
                    new InMemoryDPoPSingleUseChecker(authorizationServer.getDPoPMaxAgeSeconds() * 2, authorizationServer.getDPoPMaxAgeSeconds() * 4));
            DPoPIssuer dpopIssuer = new DPoPIssuer(clientID);
            requestVerifier.verify(
                    context.endpointHttpMethod().name(),
                    context.endpointURI(),
                    dpopIssuer,
                    dPopProof,
                    dPoPAccessToken,
                    cnf,
                    null // https://digdir.atlassian.net/browse/EUW-927 - nonce
            );
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Failed to parse DPoP access token", e);
        } catch (ParseException e) {
            throw new IssuerServerException(ErrorCode.INVALID_DPOP_PROOF, "Failed to parse DPoP proof", e);
        } catch (InvalidDPoPProofException e) {
            throw new IssuerServerException(ErrorCode.INVALID_DPOP_PROOF, "Invalid DPoP proof", e);
        } catch (AccessTokenValidationException e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Access token validation failed", e);
        } catch (JOSEException e) {
            throw new IssuerServerException(ErrorCode.INVALID_DPOP_PROOF, "Failed to validate DPoP proof", e);
        }
    }

    /**
     * Checks that a validated access token also meets the requirements of the credential configuration.
     */
    public void validateAccessTokenForCredentialConfiguration(JWT accessToken, AccessTokenCredentialValidationContext validationContext) {
        AuthorizationServer authorizationServer = validationContext.authorizationServer() != null
                ? authorizationServerService.findAuthorizationServerById(validationContext.authorizationServer())
                : authorizationServerService.findPreAuthorizationServerById(validationContext.preAuthorizationServer());
        try {
            if (! authorizationServer.getIssuer().equals(URI.create(accessToken.getJWTClaimsSet().getIssuer()))) {
                throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Invalid authorization server for credential configuration.");
            }
            if (! validateScope(validationContext.scope(), accessToken)) {
                throw new IssuerServerException(ErrorCode.INSUFFICIENT_SCOPE, "Invalid scope for credential configuration.");
            }

        } catch (ParseException e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Invalid token format.");
        }
    }

    /**
     * Checks that a validated access token is bound to subject.
     */
    public void validateAccessTokenBoundToSubject(JWT accessToken, String personIdentifier) {
        try {
            String pid = accessToken.getJWTClaimsSet().getStringClaim("pid");
            if (! StringUtils.hasText(pid)) {
                throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Token must contain person identifier in pid claim.");
            }
            if (! Objects.equals(pid, personIdentifier)) {
                throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Token and request subject/person identifier does not match.");
            }
        } catch (ParseException e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Invalid token format.");
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
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Invalid token format.");
        }
    }

}
