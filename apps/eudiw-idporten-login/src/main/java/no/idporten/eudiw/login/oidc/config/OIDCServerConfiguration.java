package no.idporten.eudiw.login.oidc.config;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import no.digdir.oidc.redis.service.RedisOpenIDConnectCache;
import no.idporten.eudiw.login.AcrValue;
import no.idporten.lib.keystore.KeystoreConfig;
import no.idporten.lib.keystore.KeystoreManager;
import no.idporten.lib.keystore.spring.KeystoreConfigurationProperties;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegrationBase;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

@Configuration
public class OIDCServerConfiguration {

    private final RedisOpenIDConnectCache cache;
    private final KeystoreManager keystoreManager;
    private final KeystoreConfigurationProperties keystoreConfigurationProperties;
    private static final String OIDC_PROVIDER_KEYSTORE_NAME = "oidc-provider";

    public OIDCServerConfiguration(RedisOpenIDConnectCache cache, KeystoreManager keystoreManager, KeystoreConfigurationProperties keystoreConfigurationProperties) {
        this.cache = cache;
        this.keystoreManager = keystoreManager;
        this.keystoreConfigurationProperties = keystoreConfigurationProperties;
    }

    @Bean
    public OpenIDConnectSdkConfiguration openIDConnectSdkConfig(OIDCServerProperties oidcServerProperties) throws Exception {
        OpenIDConnectSdkConfiguration.OpenIDConnectSdkConfigurationBuilder builder =
                OpenIDConnectSdkConfiguration.builder()
                        .issuer(oidcServerProperties.issuer())
                        .internalId(oidcServerProperties.internalId())
                        .clients(oidcServerProperties.clients().stream().map(clientMetadataProperties ->
                                ClientMetadata.builder()
                                        .clientId(clientMetadataProperties.clientId())
                                        .clientSecret(clientMetadataProperties.clientSecret())
                                        .redirectUris(clientMetadataProperties.redirectUris())
                                        .scopes(clientMetadataProperties.scopes())
                                        .build()).toList())
                        .acrValues(Arrays.stream(AcrValue.values()).map(AcrValue::value).toList())
                        .responseModes(oidcServerProperties.responseModesSupported())
                        .scopesSupported(oidcServerProperties.scopesSupported())
                        .uiLocales(oidcServerProperties.uiLocales())
                        .jwksUri(endpointUri(oidcServerProperties, "jwks"))
                        .pushedAuthorizationRequestEndpoint(endpointUri(oidcServerProperties, "par"))
                        .authorizationEndpoint(endpointUri(oidcServerProperties, "authorize"))
                        .tokenEndpoint(endpointUri(oidcServerProperties, "token"))
                        .cache(cache);
        if (isLoadKeyStore(OIDC_PROVIDER_KEYSTORE_NAME)) {
            KeystoreConfig keystoreConfig = keystoreConfigurationProperties.getKeystore(OIDC_PROVIDER_KEYSTORE_NAME);
            builder.keystore(keystoreManager.getKeystore(OIDC_PROVIDER_KEYSTORE_NAME), keystoreConfig.keyAlias(), keystoreConfig.keyPassword());
        } else {
            builder.jwk(generateServerKeys());
        }
        return builder.build();
    }

    private URI endpointUri(OIDCServerProperties oidcServerProperties, String endpoint) {
        return UriComponentsBuilder.fromUri(oidcServerProperties.issuer()).path(endpoint).build().toUri();
    }

    private RSAKey generateServerKeys() throws Exception {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate();
    }

    private boolean isLoadKeyStore(String keystoreName) {
        return keystoreManager.getKeystoreNames().contains(keystoreName);
    }

    @Bean
    public OpenIDConnectIntegration openIDConnectSdk(OpenIDConnectSdkConfiguration openIDConnectSdkConfig) {
        return new OpenIDConnectIntegrationBase(openIDConnectSdkConfig);
    }

}
