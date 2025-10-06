package no.idporten.eudiw.issuer.ui.demo.issuer.config;

import no.idporten.lib.maskinporten.client.MaskinportenClient;
import no.idporten.lib.maskinporten.client.MaskinportenClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class IssuerServerIntegration {

    private final IssuerServerProperties issuerServerProperties;

    @Autowired
    public IssuerServerIntegration(IssuerServerProperties issuerServerProperties) {
        this.issuerServerProperties = issuerServerProperties;
    }

    @Bean
    public MaskinportenClient maskinportenClient(MaskinportenClients maskinportenClients) {
        return maskinportenClients.getDefaultClient();
    }

    @Bean("issuerServerRestClient")
    public RestClient issuerServerRestClient() {
        return RestClient.builder()
                .baseUrl(issuerServerProperties.credentialIssuer())
                .build();
    }

}
