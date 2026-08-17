package no.idporten.eudiw.connector.authoritativesources.skatteetaten;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

import static no.idporten.eudiw.connector.authoritativesources.config.MaskinportenConfiguration.DEFAULT_MASKINPORTEN_CLIENT;

@Validated
@ConfigurationProperties("authoritative-sources-connector.connectors.inntektsapi")
public record InntekstsApiProperties(
        @NotNull URI uri,
        @NotEmpty String scope,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue(DEFAULT_MASKINPORTEN_CLIENT) String maskinportenClient
) {
 }
