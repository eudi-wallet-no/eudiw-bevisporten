package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.oauth2.sdk.id.Issuer;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class AuthorizationServerService implements InitializingBean {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    public AuthorizationServer findAuthorizationServer(String issuer) {
        return credentialIssuerServerProperties.getAuthorizationServers()
                .stream()
                .filter(authorizationServer -> URI.create(issuer).equals(authorizationServer.getIssuer()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (AuthorizationServer authorizationServer : credentialIssuerServerProperties.getAuthorizationServers()) {
            JWKSource<SecurityContext> jwkSource = JWKSourceBuilder
                    .create(authorizationServer.getJwksUri().toURL())
                    .cache(24 * 60 * 60 * 1000,5000)
                    .build();
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(Set.of(JWSAlgorithm.RS256, JWSAlgorithm.ES256), jwkSource);
            authorizationServer.setAccessTokenValidator(new AccessTokenValidator(new Issuer(authorizationServer.getIssuer()), keySelector));
        }
    }

}
