package no.idporten.eudiw.login.oidc.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Binding to properties for client metadata.
 */
@Data
public class ClientMetadataProperties {

    @NotNull
    String clientId;
    @NotNull
    String clientSecret;
    @NotEmpty
    List<@NotNull String> redirectUris;
    @NotEmpty
    List<@NotNull String> scopes;

}
