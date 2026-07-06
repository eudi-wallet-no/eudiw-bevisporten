package no.idporten.eudiw.login.openid4vp.verifier;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "eudiw-idporten-login.openid4vp-verifier-service")
@Data
public class VerifierServiceProperties {

    @NotNull
    private URI uri;

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(3);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(3);
}
