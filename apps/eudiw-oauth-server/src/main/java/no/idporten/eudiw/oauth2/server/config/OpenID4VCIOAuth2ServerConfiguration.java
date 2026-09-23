package no.idporten.eudiw.oauth2.server.config;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauth2.server.audit.AuditService;
import no.idporten.eudiw.oauth2.server.crypto.KeyStoreProperties;
import no.idporten.eudiw.oauth2.server.crypto.KeyStoreProvider;
import no.idporten.eudiw.oauth2.server.OpenID4VCIAuthorizationServer;
import no.idporten.eudiw.oauth2.server.cache.OpenIDConnectCache;
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


/**
 * Spring configuration configuring an embedded OAuth2 authorization server supporting OpenID for Verifiable Credential Issuance (OpenID4VCI).
 */
@Configuration
@Data
@Slf4j
@Validated
@ConfigurationProperties(prefix = "oauth-authorization-server")
public class OpenID4VCIOAuth2ServerConfiguration implements InitializingBean {

    @NotBlank
    private String apiKey;

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

    @NotNull
    private URI accessTokenDefaultAudience;

    private boolean requirePkce = true;
    private boolean requireChallenge = true;
    private boolean authorizationResponseIssParameterSupported = true;
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
    public OpenID4VCIAuthorizationServer authorizationServer(OAuth2ServerConfiguration serverConfiguration) {
        return new OpenID4VCIAuthorizationServer(serverConfiguration);
    }

    @Bean
    public OAuth2ServerConfiguration serverConfiguration(AuditService auditService, OpenIDConnectCache openIDConnectCache) throws Exception {
        OAuth2ServerConfiguration.OAuth2ServerConfigurationBuilder builder =
                OAuth2ServerConfiguration.builder()
                        .apiKey(apiKey)
                        .internalId(internalId)
                        .issuer(issuer)
                        .pushedAuthorizationRequestEndpoint(UriComponentsBuilder.fromUri(issuer).path("/par").build().toUri())
                        .authorizationEndpoint(UriComponentsBuilder.fromUri(issuer).path("/authorize").build().toUri())
                        .tokenEndpoint(UriComponentsBuilder.fromUri(issuer).path("/token").build().toUri())
                        .jwksUri(UriComponentsBuilder.fromUri(issuer).path("/jwks").build().toUri())
                        .challengeEndpoint(UriComponentsBuilder.fromUri(issuer).path("/challenge").build().toUri())
                        .grantTypesSupported(grantTypesSupported)
                        .authorizationRequestLifetimeSeconds(parLifetimeSeconds)
                        .authorizationLifetimeSeconds(authorizationLifetimeSeconds)
                        .accessTokenDefaultAudience(accessTokenDefaultAudience)
                        .authorizationResponseIssParameterSupported(authorizationResponseIssParameterSupported)
                        .requirePkce(requirePkce)
                        .requireChallenge(requireChallenge)
                        .responseMode("query")
//                        .uiLocales(uiLocales)
                        .scopesSupported(scopesSupported)
                        .authorizationDetailsTypeSupported("openid_credential")
                        .cache(openIDConnectCache)
                        .auditLogger(auditService);

        if (keyStore == null) {
            builder.jwk(generateServerECKey());
        } else {
            builder.keystore(loadServerKeystore(keyStore), keyStore.keyAlias(), keyStore.keyPassword());
        }
        log.info("Initialized authorizationb server with id {} for issuer {}", internalId, issuer);
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
