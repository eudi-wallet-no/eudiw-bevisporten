package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jose.JWSAlgorithm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import no.idporten.eudiw.issuer.config.APIConnectionProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.Set;

@Validated
@Data
public class AuthorizationServer {

    /**
     * Internal identifier for authorization server.  Used to reference authorization server as a logical entity from
     * configuration.
     */
    @NotEmpty
    private String id;

    /**
     * OAuth2 authorization server issuer.
     */
    @NotNull
    private URI issuer;

    /**
     * OAuth2 authorization server jwks uri.
     */
    @NotNull
    private URI jwksUri;

    /**
     * Allowed time skew for DPoP tokens
     */
    @Min(1)
    private long dPoPTimeSkewSeconds = 60;

    /**
     * Allowed max age for DPoP tokens
     */
    @Min(1)
    private long dPoPMaxAgeSeconds = 120;

    /**
     * Expected algorithms for DPoP tokens
     */
    @NotEmpty
    private Set<@NotNull JWSAlgorithm> dPoPAlgorithms = Set.of(JWSAlgorithm.ES256);

    /**
     * OAuth2 authorization server internal api properties.  Used in pre-authorized code flow.
     */
    private APIConnectionProperties internalApi;

    /**
     * Validator for access_tokens from issuer.
     */
    private AccessTokenValidator accessTokenValidator;

}
