package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class AccessTokenValidationService {

    private final AuthorizationServerService authorizationServerService;

    /**
     * Extracts access token from authorization header, parses JWT, checks issuer to find authorization server
     * and pre-created jwt validator for actual authorization server.
     * @param authorizationHeader
     * @return
     */
    public JWT validateAccessTokenForCredentialConfiguration(String authorizationHeader, List<AuthorizationServer> authorizationServers) {
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
            return authorizationServer.getAccessTokenValidator().validate(jwtAccessToken);
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format.", HttpStatus.UNAUTHORIZED);
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid authentication scheme.", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Validates that an access token meets the requirements of the credential configuration.
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
     * Checks that valid access token is bound to subject.
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
