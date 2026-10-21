package no.idporten.eudiw.verifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "sd-jwt")
public record SdjwtConfig(
        @DefaultValue("30s") Duration clockSkew
) {
}

