package no.idporten.eudiw.issuer.openid4vci;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialConfigurations;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialIssuerMetadata;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
@Configuration
public class CredentialIssuerServerConfiguration {

    private final AuthorizationServerService authorizationServerService;

    private final CredentialIssuerServerGeneratorService credentialIssuerServerGeneratorService;

    private static final Logger log = LoggerFactory.getLogger(CredentialIssuerServerConfiguration.class);

    @Bean
    public CredentialIssuerMetadata credentialIssuerMetadata(CredentialIssuerServerProperties credentialIssuerProperties) {
        CredentialIssuerMetadata.CredentialIssuerMetadataBuilder builder = CredentialIssuerMetadata.builder()
                .credentialIssuer(credentialIssuerProperties.getCredentialIssuer())
                .authorizationServers(credentialIssuerProperties.getAuthorizationServers().stream().map(AuthorizationServer::getIssuer).toList())
                .credentialEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.CREDENTIAL_ENDPOINT))
                .nonceEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.NONCE_ENDPOINT))
                .notificationEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.NOTIFICATION_ENDPOINT))
                .displays(credentialIssuerProperties.getDisplayNames().keySet().stream().map(locale -> Display.builder().locale(locale).name(credentialIssuerProperties.getDisplayNames().get(locale)).build()).toList());

        CredentialConfigurations credentialConfigurations = credentialIssuerServerGeneratorService.findCredentialConfigurations(credentialIssuerProperties);
        builder.credentialConfigurations(credentialConfigurations);
        return builder.build();
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
