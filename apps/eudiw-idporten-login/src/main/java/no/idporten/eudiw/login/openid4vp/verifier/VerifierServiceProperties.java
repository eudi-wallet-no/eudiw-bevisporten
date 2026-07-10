package no.idporten.eudiw.login.openid4vp.verifier;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "eudiw-idporten-login.openid4vp-verifier-service")
public record VerifierServiceProperties(
        @NotNull URI uri,
        @NotNull @DefaultValue("3s") Duration connectTimeout,
        @NotNull @DefaultValue("3s") Duration readTimeout
) {}
