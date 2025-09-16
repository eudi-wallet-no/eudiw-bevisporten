package no.idporten.eudiw.issuer.config;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.crypto.KeyProvider;
import no.idporten.eudiw.issuer.crypto.KeyStoreProperties;
import no.idporten.eudiw.issuer.crypto.KeyStoreProvider;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.metadata.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
@Configuration
public class CredentialIssuerServerConfiguration {

    private final ClaimsSourceService claimsSourceService;
    private final AuthorizationServerService authorizationServerService;

    @Bean
    public CredentialIssuerMetadata credentialIssuerMetadata(CredentialIssuerServerProperties properties) {
        CredentialIssuerMetadata.CredentialIssuerMetadataBuilder builder = CredentialIssuerMetadata.builder()
                .credentialIssuer(properties.getCredentialIssuer())
                .authorizationServers(properties.getAuthorizationServers().stream().map(AuthorizationServer::getIssuer).toList())
                .credentialEndpoint(endpointURI(properties.getCredentialIssuer(), Endpoints.CREDENTIAL_ENDPOINT))
                .nonceEndpoint(endpointURI(properties.getCredentialIssuer(), Endpoints.NONCE_ENDPOINT))
                .notificationEndpoint(endpointURI(properties.getCredentialIssuer(), Endpoints.NOTIFICATION_ENDPOINT));
        CredentialConfigurations credentialConfigurations = new CredentialConfigurations();
        for (CredentialConfigurationProperties credentialConfigurationProperties : properties.getCredentialConfigurations()) {
            ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialConfigurationProperties.getDoctype());
            ClaimsSourceMetadata claimsSourceMetadata = claimsSource.getMetadata();
            credentialConfigurations.put(
                    credentialConfigurationProperties.getIdentifier(),
                    CredentialConfiguration.builder()
                            // credential-specific config
                            .doctype(credentialConfigurationProperties.getDoctype())
                            .format(credentialConfigurationProperties.getFormat())
                            .scope(credentialConfigurationProperties.getScope())
                            .doctype(credentialConfigurationProperties.getDoctype())
                            // config from claims source
                            .display(claimsSourceMetadata.getDisplays())
                            .claims(claimsSourceMetadata.getClaims())
                            // config from issuer server
                            .cryptographicBindingMethods(properties.getCryptographicBindings())
                            .proofTypes(ProofTypes.builder().jwtProofType(JwtProofType.builder().algorithms(properties.getProofSigningAlgorithms()).build()).build())
                    .build());
        }
        builder.credentialConfigurations(credentialConfigurations);
        return builder.build();
    }

    protected URI endpointURI(URI issuerUri, String path) {
        return UriComponentsBuilder.fromUri(issuerUri).path(path).build().toUri();
    }

    @Bean(name = "credentialSigningKeyProvider")
    public KeyProvider keyProvider(CredentialIssuerServerProperties credentialIssuerServerProperties) {
        KeyStoreProperties keyStoreProperties = credentialIssuerServerProperties.getKeyStore();
        KeyStoreProvider keyStoreProvider = new KeyStoreProvider(keyStoreProperties);
        return new KeyProvider(keyStoreProvider.keyStore(), keyStoreProperties.keyAlias(), keyStoreProperties.keyPassword());
    }

    @Bean
    public RestClient preAuthorizationRestClient() {
        AuthorizationServer authorizationServer = authorizationServerService.getPrimaryAuthorizationServer();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.of(5, ChronoUnit.SECONDS));
        requestFactory.setReadTimeout(Duration.of(5, ChronoUnit.SECONDS));
        return
                RestClient.builder()
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("X-API-KEY", authorizationServer.getApiKey())
                        .baseUrl(authorizationServer.getIssuer())
                        .requestFactory(requestFactory)
                        .build();
    }

}
