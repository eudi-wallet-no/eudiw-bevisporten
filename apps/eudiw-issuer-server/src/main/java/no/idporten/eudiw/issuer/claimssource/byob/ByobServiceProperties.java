package no.idporten.eudiw.issuer.claimssource.byob;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("byob-service")
public record ByobServiceProperties(
        @NotNull URI uri,
        @NotEmpty String apiKey,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("30s") Duration cacheTtl
) {

}
