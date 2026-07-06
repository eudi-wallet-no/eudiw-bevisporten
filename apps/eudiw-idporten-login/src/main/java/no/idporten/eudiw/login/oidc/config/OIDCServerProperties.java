package no.idporten.eudiw.login.oidc.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

/**
 * Properties for OIDC server.
 */
@ConfigurationProperties(prefix = "eudiw-idporten-login.oidc-sdk")
@Data
public class OIDCServerProperties {

    String internalId = "idporten-eudiw";

    @NotNull
    URI issuer;

    @NotNull
    List<ClientMetadataProperties> clients;

    List<String> uiLocales = List.of("nb", "nn", "en", "se");

    List<String> scopesSupported = List.of("openid");

    List<String> responseModesSupported = List.of("query", "query.jwt");

    @Min(1)
    int parLifetimeSeconds = 60;

    @Min(1)
    int authorizationLifetimeSeconds = 60;

    boolean requirePkce = true;

}
