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

@RequiredArgsConstructor
@Service
public class AuthorizationServerService implements InitializingBean {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;


    public AuthorizationServer findAuthorizationServer(String issuer, List<AuthorizationServer> authorizationServers) {
        return authorizationServers
                .stream()
                .filter(authorizationServer -> URI.create(issuer).equals(authorizationServer.getIssuer()))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("invalid_token", "Unknown authorization server.", HttpStatus.UNAUTHORIZED));
    }

    public AuthorizationServer findAuthorizationServer(String issuer) {
        return findAuthorizationServer(issuer, credentialIssuerServerProperties.getAuthorizationServers());
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
