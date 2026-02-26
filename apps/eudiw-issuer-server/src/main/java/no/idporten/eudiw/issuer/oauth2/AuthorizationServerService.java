package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.oauth2.sdk.id.Issuer;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Service
public class AuthorizationServerService implements InitializingBean {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;


    /**
     * Find an authorization server by issuer uri.
     */
    public AuthorizationServer findAuthorizationServerByIssuer(String issuer, List<AuthorizationServer> authorizationServers) {
        return findAuthorizationServer((authorizationServer) -> URI.create(issuer).equals(authorizationServer.getIssuer()), authorizationServers);
    }

    /**
     * Find an authorization server by id/logical name.
     */
    public AuthorizationServer findAuthorizationServerById(String id) {
        return findAuthorizationServer((authorizationServer) -> id.equals(authorizationServer.getId()), credentialIssuerServerProperties.getAuthorizationServers());
    }

    /**
     * Find a pre-authorization server by id/logical name.
     */
    public AuthorizationServer findPreAuthorizationServerById(String id) {
        return findAuthorizationServer((authorizationServer) -> id.equals(authorizationServer.getId()), credentialIssuerServerProperties.getPreAuthorizationServers());
    }

    private AuthorizationServer findAuthorizationServer(Predicate<AuthorizationServer> predicate, List<AuthorizationServer> authorizationServers) {
        return authorizationServers
                .stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("invalid_token", "Unknown authorization server.", HttpStatus.UNAUTHORIZED));
    }

    /**
     * The first authorization server is the server accepting pre-authorizations.
     */
    public AuthorizationServer getPrimaryAuthorizationServer() {
        return credentialIssuerServerProperties.getAuthorizationServers().getFirst();
    }

    public List<AuthorizationServer> getPreAuthorizationServers() {
        return credentialIssuerServerProperties.getPreAuthorizationServers();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<AuthorizationServer> authorizationServers = new ArrayList<>();
        authorizationServers.addAll(credentialIssuerServerProperties.getAuthorizationServers());
        authorizationServers.addAll(credentialIssuerServerProperties.getPreAuthorizationServers());
        for (AuthorizationServer authorizationServer : authorizationServers) {
            JWKSource<SecurityContext> jwkSource = JWKSourceBuilder
                    .create(authorizationServer.getJwksUri().toURL())
                    .cache(24 * 60 * 60 * 1000, 5000)
                    .build();
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(Set.of(JWSAlgorithm.RS256, JWSAlgorithm.ES256), jwkSource);
            authorizationServer.setAccessTokenValidator(new AccessTokenValidator(new Issuer(authorizationServer.getIssuer()), credentialIssuerServerProperties.getCredentialIssuer(), keySelector));
        }
    }

}
