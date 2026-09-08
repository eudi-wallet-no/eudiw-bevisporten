package no.idporten.eudiw.verifier.trustlist;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TrustlistRestclient {

    private final TrustlistsProperties trustlistConfig;

    public  TrustlistRestclient(TrustlistsProperties trustlistConfig) {
        this.trustlistConfig = trustlistConfig;
    }

    @Bean
    @Qualifier("trustlist")
    public RestClient trustlistRestClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(trustlistConfig.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(trustlistConfig.readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.etsi.tsl+xml", "application/jose+json")
                .build();
    }
}