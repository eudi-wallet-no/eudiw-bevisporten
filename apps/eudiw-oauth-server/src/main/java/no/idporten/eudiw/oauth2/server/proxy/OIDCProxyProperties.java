package no.idporten.eudiw.oauth2.server.proxy;

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
import no.idporten.eudiw.oauth2.server.crypto.KeyProvider;
import no.idporten.eudiw.oauth2.server.crypto.KeyStoreProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static no.idporten.eudiw.oauth2.server.util.StringUtils.hasText;

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
    @NotNull
    private OIDCProxyProperties.HeadlessLoginProperties headlessLogin = new HeadlessLoginProperties();

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
        if (headlessLogin.enabled && !hasText(headlessLogin.syntheticPid)) {
            throw new IllegalArgumentException("oidc-proxy.headless-login.synthetic_pid must be set when headless login is enabled");
        }
        if (headlessLogin.enabled && !hasText(headlessLogin.acr)) {
            throw new IllegalArgumentException("oidc-proxy.headless-login.acr must be set when headless login is enabled");
        }
        if (headlessLogin.enabled && !hasText(headlessLogin.amr)) {
            throw new IllegalArgumentException("oidc-proxy.headless-login.amr must be set when headless login is enabled");
        }
        if (headlessLogin.enabled && headlessLogin.clientIds.isEmpty()) {
            throw new IllegalArgumentException("oidc-proxy.headless-login.client-ids must include at least one client when headless login is enabled");
        }

        JWKSource<SecurityContext> jwkSource = JWKSourceBuilder
                .create(oidcIssuer.jwksUri().toURL())
                .cache(24 * 60 * 60 * 1000,5000)
                .build();
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(Set.of(JWSAlgorithm.RS256), jwkSource);
        idTokenValidator = new IDTokenValidator(oidcIssuer.issuer(), oidcClient.getClientID(), keySelector, null);
        jarmValidator = new JARMValidator(oidcIssuer.issuer(), oidcClient.getClientID(), keySelector, null);
    }

    @Data
    public static class HeadlessLoginProperties {
        private boolean enabled = false;
        private String syntheticPid;
        private String acr;
        private String amr;
        private List<String> clientIds = List.of();
    }

}
