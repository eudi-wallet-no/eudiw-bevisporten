package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;

/**
 * Configuration properties for connecting to an external API.
 */
public record APIConnectionProperties(@NotNull URI uri,
                                      @DefaultValue("3s") Duration connectTimeout,
                                      @DefaultValue("3s") Duration readTimeout,
                                      @DefaultValue("") String apiKeyHeader,
                                      @DefaultValue("") String apiKey) {

    public boolean useApiKey() {
        return StringUtils.hasText(apiKeyHeader) && StringUtils.hasText(apiKey);
    }

    /**
     * Creates rest client with the configured properties.
     */
    public RestClient createRestClient() {
        return createRestClientBuilder().build();
    }

    /**
     * Creates rest client builder with the configured properties.
     */
    public RestClient.Builder createRestClientBuilder() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(connectTimeout());
        clientHttpRequestFactory.setReadTimeout(readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .baseUrl(uri())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(headers -> {
                    if (useApiKey()) {
                        headers.add(apiKeyHeader(), apiKey());
                    }
                });
    }

}
