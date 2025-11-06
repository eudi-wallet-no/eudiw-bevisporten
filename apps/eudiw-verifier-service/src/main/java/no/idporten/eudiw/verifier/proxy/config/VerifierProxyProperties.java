package no.idporten.eudiw.verifier.proxy.config;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
@Configuration
@ConfigurationProperties(prefix = "verifier-proxy")
public class VerifierProxyProperties {

    private String externalBaseUri;
    private String siop2ClientId;
    private String clientIdentifierScheme;

}
