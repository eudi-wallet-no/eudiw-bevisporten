package no.idporten.eudiw.login.config;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import no.digdir.oidc.redis.service.RedisOpenIDConnectCache;
import no.idporten.lib.keystore.KeystoreManager;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegrationBase;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.KeyStore;
import java.util.UUID;

@Configuration
public class OIDCServerConfiguration {

    private final RedisOpenIDConnectCache cache;
    private final KeystoreManager keystoreManager;

    public OIDCServerConfiguration(RedisOpenIDConnectCache cache, KeystoreManager keystoreManager) {
        this.cache = cache;
        this.keystoreManager = keystoreManager;
    }

    @Bean
    public OpenIDConnectSdkConfiguration openIDConnectSdkConfig(OIDCServerProperties oidcServerProperties) throws Exception {
        OpenIDConnectSdkConfiguration.OpenIDConnectSdkConfigurationBuilder builder =
                OpenIDConnectSdkConfiguration.builder()
                        .issuer(oidcServerProperties.getIssuer())
                        .internalId(oidcServerProperties.getInternalId())
                        .clients(oidcServerProperties.getClients().stream().map(clientMetadataProperties ->
                                ClientMetadata.builder()
                                        .clientId(clientMetadataProperties.getClientId())
                                        .clientSecret(clientMetadataProperties.getClientSecret())
                                        .redirectUris(clientMetadataProperties.getRedirectUris())
                                        .scopes(clientMetadataProperties.getScopes())
                                        .build()).toList())
                        .acrValues(oidcServerProperties.getAcrValues())
                        .responseModes(oidcServerProperties.getResponseModesSupported())
                        .scopesSupported(oidcServerProperties.getScopesSupported())
                        .uiLocales(oidcServerProperties.getUiLocales())
                        .jwksUri(endpointUri(oidcServerProperties, "jwks"))
                        .pushedAuthorizationRequestEndpoint(endpointUri(oidcServerProperties, "par"))
                        .authorizationEndpoint(endpointUri(oidcServerProperties, "authorize"))
                        .tokenEndpoint(endpointUri(oidcServerProperties, "token"))
                        .cache(cache);
        builder.jwk(generateServerKeys());
        return builder.build();
    }

    private URI endpointUri(OIDCServerProperties oidcServerProperties, String endpoint) {
        return UriComponentsBuilder.fromUri(oidcServerProperties.getIssuer()).path(endpoint).build().toUri();
    }

    private RSAKey generateServerKeys() throws Exception {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate();
    }

    // TODO sdk er ikke helt kompatibelt med key store manager.  Det må vi fikse/ta via ID-porten-teamet.
    private KeyStore loadKeyStore() throws Exception {
        return keystoreManager.getKeystore("oidc-provider");
    }

    @Bean
    public OpenIDConnectIntegration openIDConnectSdk(OpenIDConnectSdkConfiguration openIDConnectSdkConfig) {
        return new OpenIDConnectIntegrationBase(openIDConnectSdkConfig);
    }

}
