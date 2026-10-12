package no.idporten.eudiw.verifier.certificateauthority;

import jakarta.validation.constraints.NotBlank;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "certificate-authority")
public record CertificateAuthorityConfig (
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @NotNull URI certificateAuthorityUrl
        ){}