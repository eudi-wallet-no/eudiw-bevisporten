package no.idporten.eudiw.issuer.config;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;
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
                .notificationEndpoint(endpointURI(properties.getCredentialIssuer(), Endpoints.NOTIFICATION_ENDPOINT))
                .displays(properties.getDisplayNames().keySet().stream().map(locale -> Display.builder().locale(locale).name(properties.getDisplayNames().get(locale)).build()).toList());
        CredentialConfigurations credentialConfigurations = new CredentialConfigurations();
        for (CredentialConfigurationProperties credentialConfigurationProperties : properties.getCredentialConfigurations()) {
            ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialConfigurationProperties.getCredentialType());
            ClaimsSourceMetadata claimsSourceMetadata = claimsSource.getMetadata();
            CredentialConfiguration.CredentialConfigurationBuilder credentialConfigurationBuilder = CredentialConfiguration.builder()
                    // credential-specific config
                    .format(credentialConfigurationProperties.getFormat().formatIdentifier())
                    .scope(credentialConfigurationProperties.getScope())
                    // config from claims source
                    .display(claimsSourceMetadata.getDisplays())
                    .claims(claimsSourceMetadata.getClaims())
                    // config from issuer server
                    .cryptographicBindingMethods(properties.getCryptographicBindings())
                    .credentialSigningAlgValuesSupported(properties.getCredentialSigningAlgorithms())
                    .proofTypes(ProofTypes.builder().jwtProofType(JwtProofType.builder().algorithms(properties.getProofSigningAlgorithms()).build()).build());
            // config for formats
            if (CredentialFormat.MSO_MDOC.equals(credentialConfigurationProperties.getFormat())) {
                credentialConfigurationBuilder.doctype(credentialConfigurationProperties.getCredentialType());
            } else if (CredentialFormat.SD_JWT_VC.equals(credentialConfigurationProperties.getFormat())) {
                credentialConfigurationBuilder.vct(credentialConfigurationProperties.getCredentialType());
            }
            credentialConfigurations.put(credentialConfigurationProperties.getIdentifier(), credentialConfigurationBuilder.build());
        }
        builder.credentialConfigurations(credentialConfigurations);
        return builder.build();
    }

    protected URI endpointURI(URI issuerUri, String path) {
        return UriComponentsBuilder.fromUri(issuerUri).path(path).build().toUri();
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
