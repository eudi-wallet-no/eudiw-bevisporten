package no.idporten.eudiw.issuer.claimssource.krr;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("krr")
public record KrrProperties(
        @NotNull URI uri,
        @NotEmpty String scope,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout
) {

}
