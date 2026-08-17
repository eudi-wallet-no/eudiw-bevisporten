package no.idporten.eudiw.connector.authoritativesources.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authoritative-sources-connector.features")
public record PersonIdentifierValidationProperties(
        boolean allowSyntheticPid,
        boolean allowRealPid
) { }