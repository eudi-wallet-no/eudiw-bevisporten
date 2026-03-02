package no.idporten.eudiw.issuer.issuance.preauth.integration;

import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class PreAuthorizedIntegrationConfiguration {

    private final AuthorizationServerService authorizationServerService;

    public PreAuthorizedIntegrationConfiguration(AuthorizationServerService authorizationServerService) {
        this.authorizationServerService = authorizationServerService;
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
