package no.idporten.eudiw.connector.authoritativesources.krr;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static no.idporten.eudiw.connector.authoritativesources.config.MaskinportenConfiguration.DEFAULT_MASKINPORTEN_CLIENT;

@Validated
@ConfigurationProperties("authoritative-sources-connector.connectors.krr")
public record KrrProperties(
        @NotNull URI uri,
        @NotEmpty String scope,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue(DEFAULT_MASKINPORTEN_CLIENT) String maskinportenClient
) {
    public List<String> scopeAsList() {
        return this.scope != null && !this.scope.isBlank() ? List.of(this.scope.split(" ")) : List.of();
    }
}
