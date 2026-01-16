package no.idporten.eudiw.issuer.ui.demo.byob;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ByobConfig {

    private final ByobServiceProperties byobServiceProperties;

    public ByobConfig(ByobServiceProperties byobServiceProperties) {
        this.byobServiceProperties = byobServiceProperties;
    }

    @Bean("byobServiceRestClient")
    public RestClient byobServiceRestClient() {
        return RestClient.builder()
                .baseUrl(byobServiceProperties.baseUrl())
                .build();
    }

}
