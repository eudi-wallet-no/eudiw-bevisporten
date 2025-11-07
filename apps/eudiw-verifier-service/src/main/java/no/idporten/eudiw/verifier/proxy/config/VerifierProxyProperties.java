package no.idporten.eudiw.verifier.proxy.config;


import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
@Component
@Configuration
@ConfigurationProperties(prefix = "verifier-proxy")
public class VerifierProxyProperties {

    @NotEmpty
    private String externalBaseUri;
    @NotEmpty
    private String siop2ClientId;
    @NotEmpty
    private String clientIdentifierScheme;
    @NotEmpty
    List<String> credentialIssuers;

}
