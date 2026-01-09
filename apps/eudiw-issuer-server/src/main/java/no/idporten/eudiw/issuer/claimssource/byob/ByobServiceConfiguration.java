package no.idporten.eudiw.issuer.claimssource.byob;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ByobServiceConfiguration {

    private final ByobServiceProperties byobServiceProperties;

    public ByobServiceConfiguration(ByobServiceProperties byobServiceProperties) {
        this.byobServiceProperties = byobServiceProperties;
    }

    @Bean
    public RestClient byobServiceRestClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(byobServiceProperties.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(byobServiceProperties.readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .baseUrl(byobServiceProperties.uri())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}
