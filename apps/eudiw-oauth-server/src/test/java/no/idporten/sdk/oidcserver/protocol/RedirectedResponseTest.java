package no.idporten.sdk.oidcserver.protocol;

import static no.idporten.sdk.oidcserver.util.MultiValuedMapUtils.*;
import no.idporten.sdk.oidcserver.util.URIUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("When using a redirected response")
public class RedirectedResponseTest {

    @Test
    @DisplayName("then response parameters are added to the redirect_uri")
    public void testBuildRedirectUri() {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .responseMode("query")
                .redirectUri("https://www.idporten.no/")
                .state("sss")
                .code("c")
                .iss("i")
                .build();
        authorizationResponse.addParameter("empty", null);
        authorizationResponse.addParameter("any", "thing");
        authorizationResponse.addParameter("code", "cantoverride");
        RedirectedResponse redirectedResponse = new RedirectedResponse(authorizationResponse.getRedirectUri(), authorizationResponse.toResponseParameters());
        URI uri = redirectedResponse.toQueryRedirectUri();
        assertAll(
                () -> assertEquals("https", uri.getScheme()),
                () -> assertEquals("www.idporten.no", uri.getAuthority())
        );
        Map<String, List<String>> responseParameters = URIUtils.parseParameters(uri.getQuery());
        assertAll(
                () -> assertEquals("thing", getFirstValue("any", responseParameters)),
                () -> assertEquals("sss", getFirstValue("state",responseParameters)),
                () -> assertEquals("c", getFirstValue("code",responseParameters)),
                () -> assertEquals("i", getFirstValue("iss",responseParameters)),
                () -> assertFalse(responseParameters.containsKey("empty")),
                () -> assertFalse(responseParameters.containsKey("aud"))
        );
    }

}
