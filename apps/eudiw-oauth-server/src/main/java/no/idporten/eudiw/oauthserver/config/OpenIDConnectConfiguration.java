package no.idporten.eudiw.oauthserver.config;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauthserver.audit.AuditService;
import no.idporten.eudiw.oauthserver.crypto.KeyStoreProperties;
import no.idporten.eudiw.oauthserver.crypto.KeyStoreProvider;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegrationBase;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.KeyStore;
import java.util.List;
import java.util.UUID;

@Configuration
@Data
@Slf4j
@Validated
@ConfigurationProperties(prefix = "oauth-authorization-server")
public class OpenIDConnectConfiguration implements InitializingBean {

    private String internalId = "eudiw";

    @NotNull
    private URI issuer;

//    @NotEmpty
//    private List<String> uiLocales;

    @NotEmpty
    private List<String> grantTypesSupported;

    @NotEmpty
    private List<String> scopesSupported;

//    @NotEmpty
//    private List<String> responseModesSupported = new ArrayList<>();

    @Min(1)
    private int parLifetimeSeconds = 60;
    @Min(1)
    private int authorizationLifetimeSeconds = 60;
    private boolean requirePkce = true;
    private KeyStoreProperties keyStore;

    @Override
    public void afterPropertiesSet() {
        if (keyStore != null) {
        }
    }

    protected void notEmptyForServerKeystore(String property, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Property %s must have a value when using a server keystore".formatted(property));
        }
    }

    @Bean
    public OpenIDConnectIntegration openIDConnectSdk(OpenIDConnectSdkConfiguration openIDConnectSdkConfig) {
        return new OpenIDConnectIntegrationBase(openIDConnectSdkConfig);
    }

    @Bean
    public OpenIDConnectSdkConfiguration openIDConnectSdkConfig(AuditService auditService) throws Exception {
        OpenIDConnectSdkConfiguration.OpenIDConnectSdkConfigurationBuilder builder =
                OpenIDConnectSdkConfiguration.builder()
                        .internalId(internalId)
                        .issuer(issuer)
                        .pushedAuthorizationRequestEndpoint(UriComponentsBuilder.fromUri(issuer).path("/par").build().toUri())
                        .authorizationEndpoint(UriComponentsBuilder.fromUri(issuer).path("/authorize").build().toUri())
                        .tokenEndpoint(UriComponentsBuilder.fromUri(issuer).path("/token").build().toUri())
                        .jwksUri(UriComponentsBuilder.fromUri(issuer).path("/jwks").build().toUri())
                        .grantTypesSupported(grantTypesSupported)
                        .authorizationRequestLifetimeSeconds(parLifetimeSeconds)
                        .authorizationLifetimeSeconds(authorizationLifetimeSeconds)
                        .requirePkce(requirePkce)
                        .responseMode("query")
//                        .uiLocales(uiLocales)
                        .scopesSupported(scopesSupported)
                        .authorizationDetailsTypeSupported("openid_credential")
                        .cache(new SimpleOpenIDConnectCache())
                        .auditLogger(auditService);

        if (keyStore == null) {
            builder.jwk(generateServerECKey());
        } else {
            builder.keystore(loadServerKeystore(keyStore), keyStore.keyAlias(), keyStore.keyPassword());
        }
        log.info("Initialized OIDC SDK with id {} for issuer {}", internalId, issuer);
        return builder.build();
    }

    public ECKey generateServerECKey() throws Exception {
        ECKey ecKey = new ECKeyGenerator(Curve.P_256)
                .keyUse(KeyUse.SIGNATURE)
                .keyIDFromThumbprint(true)
                .keyID(UUID.randomUUID().toString())
                .generate();
        log.info("Generated server keys for signing av tokens.");
        return ecKey;
    }

    public KeyStore loadServerKeystore(KeyStoreProperties keyStoreProperties) {
        return new KeyStoreProvider(keyStoreProperties).keyStore();
    }

}
