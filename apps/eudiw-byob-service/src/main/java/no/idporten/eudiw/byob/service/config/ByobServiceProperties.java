package no.idporten.eudiw.byob.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "byob-service")
public record ByobServiceProperties(String apiKey) {
}
