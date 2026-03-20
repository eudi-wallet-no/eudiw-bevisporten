package no.idporten.eudiw.connector.authoritativesources.freg;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("authoritative-sources-connector.connectors.freg")
public record FregProperties(
        @NotNull URI uri,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout
) {

}
