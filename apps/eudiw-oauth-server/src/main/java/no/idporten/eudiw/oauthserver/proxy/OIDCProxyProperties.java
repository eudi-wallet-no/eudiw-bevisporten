package no.idporten.eudiw.oauthserver.proxy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.jarm.JARMValidator;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauthserver.crypto.KeyProvider;
import no.idporten.eudiw.oauthserver.crypto.KeyStoreProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.Set;

@Configuration
@Data
@Slf4j
@Validated
@ConfigurationProperties(prefix = "oidc-proxy")
public class OIDCProxyProperties implements InitializingBean {

    @Min(1)
    private int connectTimeoutMillis = 5000;
    @Min(1)
    private int readTimeoutMillis = 5000;
    @NotNull
    private URI redirectUri;
    @NotNull
    private OIDCIssuerProperties oidcIssuer;
    @NotNull
    private OIDCClientProperties oidcClient;

    private IDTokenValidator idTokenValidator;
    private JARMValidator jarmValidator;

    @Override
    public void afterPropertiesSet() throws Exception {
        oidcClient.validate();
        if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(oidcClient.getClientAuthenticationMethod())) {
            KeyStoreProvider keyStoreProvider = new KeyStoreProvider(oidcClient.getKeystore());
            KeyProvider keyProvider = new KeyProvider(keyStoreProvider.keyStore(), oidcClient.getKeystore().keyAlias(), oidcClient.getKeystore().keyPassword());
            oidcClient.setKeyProvider(keyProvider);
        }

        JWKSource<SecurityContext> jwkSource = JWKSourceBuilder
                .create(oidcIssuer.jwksUri().toURL())
                .cache(24 * 60 * 60 * 1000,5000)
                .build();
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(Set.of(JWSAlgorithm.RS256), jwkSource);
        idTokenValidator = new IDTokenValidator(oidcIssuer.issuer(), oidcClient.getClientID(), keySelector, null);
        jarmValidator = new JARMValidator(oidcIssuer.issuer(), oidcClient.getClientID(), keySelector, null);
    }

}
