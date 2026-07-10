package no.idporten.eudiw.login.oidc.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.util.List;

/**
 * Properties for OIDC server.
 */
@ConfigurationProperties(prefix = "eudiw-idporten-login.oidc-sdk")
public record OIDCServerProperties(
        @DefaultValue("idporten-eudiw") String internalId,
        @NotNull URI issuer,
        @NotNull List<ClientMetadataProperties> clients,
        @DefaultValue("nb,nn,en,se") List<String> uiLocales,
        @DefaultValue("openid") List<String> scopesSupported,
        @DefaultValue("query,query.jwt") List<String> responseModesSupported,
        @Min(1) @DefaultValue("60") int parLifetimeSeconds,
        @Min(1) @DefaultValue("60") int authorizationLifetimeSeconds,
        @DefaultValue("true") boolean requirePkce
) {}
