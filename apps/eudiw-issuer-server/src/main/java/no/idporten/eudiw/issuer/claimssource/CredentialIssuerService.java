package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CredentialIssuerService {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final ClaimsSourceService claimsSourceService;

    public List<Credential> issueCredentials(CredentialRequest credentialRequest, JWT accessToken) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(credentialRequest.getCredentialConfigurationId());
        if (! validate(credentialConfigurationProperties.getAuthorizationServer(), accessToken)) {
            throw new IssuerServerException("invalid_token", "Invalid authorization server for credential configuration", HttpStatus.UNAUTHORIZED);
        }
        if (! validateScope(credentialConfigurationProperties.getScope(), accessToken)) {
            throw new IssuerServerException("insufficient_scope", "Invalid scope for credential configuration", HttpStatus.FORBIDDEN);
        }
        ClaimsSource claimsSource = claimsSourceService.findCredentialClaimsSource(credentialConfigurationProperties.getDoctype());
        List<Claim> claims = claimsSource.retrieveClaims(accessToken);
        return List.of(new Credential(claims.stream().map(claim -> claim.getPath() + "=" + claim.getValue()).collect(Collectors.joining(","))));
    }

    private boolean validate(String authorizationServer, JWT accessToken) {
        try {
            return authorizationServer.equals(accessToken.getJWTClaimsSet().getIssuer());
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean validateScope(String expectedScope, JWT accessToken) {
        try {
            String scope = accessToken.getJWTClaimsSet().getStringClaim("scope");
            if (StringUtils.isEmpty(scope)) {
                return false;
            }
            List<String> parsedScopes = List.of(scope.split("\\s+"));
            return parsedScopes.contains(expectedScope);
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format", HttpStatus.UNAUTHORIZED);
        }
    }

}
