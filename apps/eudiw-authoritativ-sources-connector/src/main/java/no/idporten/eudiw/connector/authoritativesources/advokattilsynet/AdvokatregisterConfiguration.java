package no.idporten.eudiw.connector.authoritativesources.advokattilsynet;

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
@EnableConfigurationProperties(AdvokatregisteretProperties.class)
public class AdvokatregisterConfiguration {

    private final AdvokatregisteretProperties advokatregisteretProperties;

    public AdvokatregisterConfiguration(AdvokatregisteretProperties advokatregisteretProperties) {
        this.advokatregisteretProperties = advokatregisteretProperties;
    }

    @Bean
    public RestClient advokatregisteretRestClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(advokatregisteretProperties.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(advokatregisteretProperties.readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .baseUrl(advokatregisteretProperties.uri())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean(value="advokatregisteretMaskinportenClient")
    public MaskinportenClient advokatregisteretMaskinportenClient(MaskinportenClients maskinportenClients) {
        return maskinportenClients.getClient("advokatregisteret");
    }

}
