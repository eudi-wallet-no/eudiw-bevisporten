package no.idporten.sdk.oidcserver.protocol;

import lombok.Getter;
import static no.idporten.sdk.oidcserver.util.MultiValuedMapUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Representation of an authorization request.  In the pushed authorization request use case, only client_id and
 * request_uri-parameters are used.
 *
 * See https://tools.ietf.org/html/draft-lodderstedt-oauth-par-00#section-4 .
 * See https://tools.ietf.org/html/draft-ietf-oauth-jwsreq-31#sectio .
 */
@Getter
public class AuthorizationRequest implements AuditDataProvider {

    // OAuth2 request parameter
    private String requestUri;
    private String clientId;
    // HTTP headers
    private String userAgentHeader;

    public AuthorizationRequest(final Map<String, List<String>> headers, final Map<String, List<String>> parameters) {
        Map<String, List<String>> ciHeaders = caseInsensitiveMap(headers);
        clientId = getFirstValue("client_id", parameters);
        requestUri = getFirstValue("request_uri", parameters);
        userAgentHeader = extractUserAgent(ciHeaders);
    }

    protected String extractUserAgent(Map<String, List<String>> headers) {
        return Optional.ofNullable(getFirstValue("User-Agent", headers)).map(s -> "%1.256s".formatted(s)).orElse(null);
    }

    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
                .attribute("client_id", clientId)
                .attribute("request_uri", requestUri)
                .attribute("user_agent", userAgentHeader)
                .build();
    }

}
