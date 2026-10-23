package no.idporten.eudiw.verifier.statuslist;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TokenStatuslistRestClient {

    private final TokenStatuslistConfig tokenStatuslistConfig;
    public TokenStatuslistRestClient(TokenStatuslistConfig tokenStatuslistConfig) {
        this.tokenStatuslistConfig = tokenStatuslistConfig;
    }

    @Bean("tokenStatuslist")
    public RestClient restClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(tokenStatuslistConfig.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(tokenStatuslistConfig.readTimeout());

        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .build();
    }

}
