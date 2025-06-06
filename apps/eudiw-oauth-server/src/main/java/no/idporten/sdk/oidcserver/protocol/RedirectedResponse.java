package no.idporten.sdk.oidcserver.protocol;

import lombok.SneakyThrows;
import no.idporten.sdk.oidcserver.util.URIUtils;

import java.net.URI;
import java.util.Map;

/**
 * Response data and formatting for query responses.
 */
public final class RedirectedResponse extends ClientResponse {

    public RedirectedResponse(String redirectUri, Map<String, String> parameters) {
        super(redirectUri, parameters);
    }

    @SneakyThrows
    public URI toQueryRedirectUri() {
        return URIUtils.appendQuery(new URI(getRedirectUri()), getParameters());
    }

}
