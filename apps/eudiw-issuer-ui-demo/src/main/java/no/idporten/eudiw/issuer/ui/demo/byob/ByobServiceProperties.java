package no.idporten.eudiw.issuer.ui.demo.byob;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(ByobServiceProperties.class)
@ConfigurationProperties(prefix = "bevisgenerator.byob-service")
public record ByobServiceProperties(
        String baseUrl,
        String createEndpoint,
        String getManyEndpoint,
        String getEndpoint,
        String searchEndpoint
) { }
