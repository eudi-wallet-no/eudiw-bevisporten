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
import no.idporten.eudiw.oauthserver.crypto.KeyStoreProvider;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegrationBase;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
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
//    private List<ClientMetadata> clients;
//    @NotEmpty
//    private List<String> uiLocales;
//    @NotEmpty
//    private List<String> acrValues;
    @NotEmpty
    private List<String> scopesSupported;
//    private List<String> claimsSupported = new ArrayList<>();
//    @NotEmpty
//    private List<String> responseModesSupported = new ArrayList<>();
    @Min(1)
    private int parLifetimeSeconds = 60;
    @Min(1)
    private int authorizationLifetimeSeconds = 60;
    private String keystoreType;
    private String keystoreLocation;
    private String keystorePassword;
    private String keystoreKeyAlias;
    private String keystoreKeyPassword;
    private boolean requirePkce = true;

    @Override
    public void afterPropertiesSet() {
        if (StringUtils.hasText(keystoreType)) {
            notEmptyForServerKeystore("keystoreLocation", keystoreLocation);
            notEmptyForServerKeystore("keystorePassword", keystorePassword);
            notEmptyForServerKeystore("keystoreKeyAlias", keystoreKeyAlias);
            notEmptyForServerKeystore("keystoreKeyPassword", keystoreKeyPassword);
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
    public OpenIDConnectSdkConfiguration openIDConnectSdkConfig() throws Exception {
        // TODO
        OpenIDConnectSdkConfiguration.OpenIDConnectSdkConfigurationBuilder builder =
                OpenIDConnectSdkConfiguration.builder()
                        .internalId(internalId)
                        .issuer(issuer)
                        .pushedAuthorizationRequestEndpoint(UriComponentsBuilder.fromUri(issuer).path("/par").build().toUri())
                        .authorizationEndpoint(UriComponentsBuilder.fromUri(issuer).path("/authorize").build().toUri())
                        .tokenEndpoint(UriComponentsBuilder.fromUri(issuer).path("/token").build().toUri())
                        .jwksUri(UriComponentsBuilder.fromUri(issuer).path("/jwks").build().toUri())
                        .authorizationRequestLifetimeSeconds(parLifetimeSeconds)
                        .authorizationLifetimeSeconds(authorizationLifetimeSeconds)
                        .requirePkce(requirePkce)
//                        .acrValues(acrValues)
                        .responseMode("query")
//                        .uiLocales(uiLocales)
                        .scopesSupported(scopesSupported)
//                        .claimsSupported(claimsSupported)
//                        .clients(clients);
                        .cache(new SimpleOpenIDConnectCache())
        ;
        if (keystoreType == null) {
            builder.jwk(generateServerECKey());
        } else {
            builder.keystore(loadServerKeystore(), keystoreKeyAlias, keystoreKeyPassword);
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

    public KeyStore loadServerKeystore() throws Exception {
        KeyStore keyStore = new KeyStoreProvider(
                keystoreType,
                keystoreLocation,
                keystorePassword,
                new DefaultResourceLoader())
                .keyStore();
        log.info("Loaded server keystore from {}", keystoreLocation);
        return keyStore;
    }

}
