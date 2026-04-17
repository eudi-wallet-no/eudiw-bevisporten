package no.idporten.eudiw.statuslist.issuer.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "status-list.issuer")
public record StatusIssuerProperties(@NotBlank String apiKey) {
}
