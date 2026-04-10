package no.idporten.eudiw.connector.authoritativesources.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "authoritative-sources-connector.api")
public record ApiProperties(@NotBlank String apiKey) { }
