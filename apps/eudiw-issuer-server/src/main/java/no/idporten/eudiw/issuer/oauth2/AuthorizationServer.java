package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jose.JWSAlgorithm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.Set;

@Validated
@Data
public class AuthorizationServer {

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
     * OAuth2 authorization server api key for internal api.  Used in pre authorized flow.
     */
    private String apiKey;

    /**
     * Validator for access_tokens from issuer.
     */
    private AccessTokenValidator accessTokenValidator;

}
