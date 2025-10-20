package no.idporten.eudiw.issuer.claimssource.krr;

import no.idporten.lib.maskinporten.client.MaskinportenClient;
import no.idporten.lib.maskinporten.client.MaskinportenClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class KrrConfiguration {

    private final KrrProperties krrProperties;

    public KrrConfiguration(KrrProperties krrProperties) {
        this.krrProperties = krrProperties;
    }

    @Bean
    public RestClient krrRestClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(krrProperties.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(krrProperties.readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .baseUrl(krrProperties.uri())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean(value="krrMaskinportenClient")
    public MaskinportenClient krrMaskinportenClient(MaskinportenClients maskinportenClients) {
        return maskinportenClients.getClient("krr");
    }

}
