package no.idporten.eudiw.login.openid4vp.wallet;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "eudiw-idporten-login.wallet-interaction")
public record WalletInteractionProperties(
        @NotNull @DefaultValue("5s") Duration pollingInterval,
        @NotNull @DefaultValue("10m") Duration pollingTimeout

) {}

