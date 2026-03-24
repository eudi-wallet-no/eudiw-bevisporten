package no.idporten.eudiw.connector.authoritativesources.skatteetaten;

import no.idporten.lib.maskinporten.client.MaskinportenClient;
import no.idporten.lib.maskinporten.client.MaskinportenClients;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(InntekstsApiProperties.class)
public class InntekstApiConfiguration {

    private final InntekstsApiProperties inntekstsApiProperties;

    public InntekstApiConfiguration(InntekstsApiProperties inntekstsApiProperties) {
        this.inntekstsApiProperties = inntekstsApiProperties;
    }

    @Bean
    public RestClient inntekstApiRestClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(inntekstsApiProperties.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(inntekstsApiProperties.readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .baseUrl(inntekstsApiProperties.uri())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean(value = "inntektsApiMaskinportenClient")
    public MaskinportenClient inntektsApiMaskinportenClient(MaskinportenClients maskinportenClients) {
        return maskinportenClients.getClient("inntektsapi");
    }
}
