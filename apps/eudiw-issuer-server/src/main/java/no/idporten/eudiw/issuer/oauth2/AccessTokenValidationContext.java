package no.idporten.eudiw.issuer.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.List;

/**
 * Context for access_token validation.
 *
 * @param authorizationHeader    authorization header with bearer or dPoP token
 * @param dpopHeader             DPoP header for dPoP tokens
 * @param endpointHttpMethod     endpoint HTTP method for dPoP validation
 * @param endpointURI            endpoint URI for dPoP validation
 * @param audience               audience for access_token validation
 * @param authorizationServers   accepted issuers of access_token
 * @param dPoPRequired           if DPoP is required
 */
public record AccessTokenValidationContext(
        String authorizationHeader,
        String dpopHeader,
        HttpMethod endpointHttpMethod,
        URI endpointURI,
        URI audience,
        List<AuthorizationServer> authorizationServers,
        boolean dPoPRequired
) {

    private static final String DPOP_HEADER = "DPoP";

    private static AccessTokenValidationContext newInstance(HttpServletRequest request, List<AuthorizationServer> authorizationServers, URI audience, boolean dPoPRequired) {
        return new AccessTokenValidationContext(
                request.getHeader(HttpHeaders.AUTHORIZATION),
                request.getHeader(DPOP_HEADER),
                HttpMethod.valueOf(request.getMethod()),
                URI.create(request.getRequestURL().toString()),
                audience,
                authorizationServers,
                dPoPRequired);
    }

    /**
     * Validation context for endpoint secured with bearer token authentication scheme.
     */
    public static AccessTokenValidationContext forBearerToken(HttpServletRequest request, List<AuthorizationServer> authorizationServers, URI audience) {
        return newInstance(request, authorizationServers, audience, false);
    }

    /**
     * Validation context for endpoint secured with DPoP authentication scheme.
     */
    public static AccessTokenValidationContext forDPoPToken(HttpServletRequest request, List<AuthorizationServer> authorizationServers, URI audience) {
        return newInstance(request, authorizationServers, audience, true);
    }

}
