package no.idporten.eudiw.issuer.issuance.preauth.integration;

import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PreAuthorizedIntegrationConfiguration {

    private final AuthorizationServerService authorizationServerService;

    public PreAuthorizedIntegrationConfiguration(AuthorizationServerService authorizationServerService) {
        this.authorizationServerService = authorizationServerService;
    }

    @Bean
    public RestClient preAuthorizationRestClient() {
        AuthorizationServer authorizationServer = authorizationServerService.getPrimaryAuthorizationServer();
        if (authorizationServer.getInternalApi() == null) {
            throw new IllegalStateException("Primary authorization server must have internal API configuration");
        }
        return authorizationServer.getInternalApi().createRestClient();
    }

}
