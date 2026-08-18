package no.idporten.eudiw.verifier.statuslist;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TokenStatuslistRestClient {

    private final TokenStatuslistConfig tokenStatuslistConfig;
    protected static final String STATUS_LIST_MEDIA_TYPE = "application/statuslist+jwt";

    public TokenStatuslistRestClient(TokenStatuslistConfig tokenStatuslistConfig) {
        this.tokenStatuslistConfig = tokenStatuslistConfig;
    }

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(tokenStatuslistConfig.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(tokenStatuslistConfig.readTimeout());

        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, STATUS_LIST_MEDIA_TYPE)
                .build();
    }

}
