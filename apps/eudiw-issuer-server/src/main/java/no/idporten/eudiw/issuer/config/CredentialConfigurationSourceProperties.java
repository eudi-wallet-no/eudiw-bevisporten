package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
public record CredentialConfigurationSourceProperties(
        @NotNull String uri,
        String apiKey,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("30s") Duration cacheTtl
) {

}
