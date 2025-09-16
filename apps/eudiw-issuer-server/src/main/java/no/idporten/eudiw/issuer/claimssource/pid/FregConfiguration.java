package no.idporten.eudiw.issuer.claimssource.pid;

import no.idporten.lib.maskinporten.client.JwtGrantTokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FregConfiguration {

    private final String fregUri;

    public FregConfiguration(@Value("${freg.uri}") String fregUri) {
        this.fregUri = fregUri;
    }

    @Bean("fregRestClient")
    public RestClient issuerServerRestClient(JwtGrantTokenInterceptor jwtGrantTokenInterceptor) {
        return RestClient.builder()
                .baseUrl(fregUri)
                .requestInterceptor(jwtGrantTokenInterceptor)
                .build();
    }



}
