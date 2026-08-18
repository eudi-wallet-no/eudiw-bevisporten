package no.idporten.eudiw.bevisgenerator.integration.verifierservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(VerificationProperties.class)
public class VerificationConfiguration {

    @Bean("verificationServiceRestClient")
    public RestClient restClient(VerificationProperties verificationProperties) {
       return RestClient.builder()
               .baseUrl(verificationProperties.baseUrl())
               .build();
    }
}
