package no.idporten.sdk.oidcserver.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Base class for client responses.  Instances contain data for the response.  Extensions contain methods to help
 * formatting the correct response to the client.
 */
@RequiredArgsConstructor
@Getter
public sealed class ClientResponse permits RedirectedResponse, FormPostResponse {

    private final String redirectUri;
    private final Map<String, String> parameters;

}
