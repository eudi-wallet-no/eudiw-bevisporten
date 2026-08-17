package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.oauth2.server.cache.SimpleOpenIDConnectCache;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.AuthorizationResponse;
import no.idporten.eudiw.oauth2.server.protocol.ClientResponse;
import no.idporten.eudiw.oauth2.server.protocol.RedirectedResponse;
import no.idporten.eudiw.oauth2.server.util.MultiValuedMapUtils;
import no.idporten.eudiw.oauth2.server.util.URIUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When creating a client response for an authorization response")
public class CreateClientResponseTest {

    private OAuth2AuthorizationServerBase oAuth2AuthorizationServer;
    private ClientMetadata client1;
    private SimpleOpenIDConnectCache cache;

    @BeforeEach
    public void setUp() throws Exception {
        client1 = ClientMetadata.builder().clientId("client1").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .client(client1)
                .build();
        oAuth2AuthorizationServer = new OAuth2AuthorizationServerBase(serverConfiguration);
        cache = (SimpleOpenIDConnectCache) serverConfiguration.getCache();
    }

    @Test
    @DisplayName("then response_mode=query.jwt generates a signed response")
    public void testSignedResponse() throws Exception {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .responseMode("query.jwt")
                .iss(oAuth2AuthorizationServer.getOpenIDProviderMetadata().getIssuer().toString())
                .redirectUri(client1.getRedirectUris().get(0))
                .aud(client1.getClientId())
                .state("sss")
                .code("c")
                .build();

        ClientResponse clientResponse = oAuth2AuthorizationServer.createClientResponse(authorizationResponse);
        assertTrue(clientResponse instanceof RedirectedResponse);
        RedirectedResponse redirectedResponse = (RedirectedResponse) clientResponse;
        Map<String, List<String>> responseParameters = URIUtils.parseParameters(redirectedResponse.toQueryRedirectUri().getQuery());
        String jwt = MultiValuedMapUtils.getFirstValue("response", responseParameters);
        JWKSet jwkSet = oAuth2AuthorizationServer.getPublicJWKSet();
        SignedJWT signedJWT = SignedJWT.parse(jwt);
        JWSHeader jwtHeader = signedJWT.getHeader();
        JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
        assertAll(
                () -> assertEquals(jwkSet.getKeys().get(0).getKeyID(), jwtHeader.getKeyID()),
                () -> assertTrue(signedJWT.verify(
                        new DefaultJWSVerifierFactory().createJWSVerifier(
                                jwtHeader,
                                jwkSet.getKeyByKeyId(jwtHeader.getKeyID()).toECKey().toKeyPair().getPublic()    ))),
                () -> assertEquals(oAuth2AuthorizationServer.getConfiguration().getIssuer().toString(), jwtClaimsSet.getIssuer()),
                () -> assertEquals(client1.getClientId(), jwtClaimsSet.getAudience().get(0)),
                () -> assertEquals("c", jwtClaimsSet.getStringClaim("code")),
                () -> assertEquals("sss", jwtClaimsSet.getStringClaim("state")),
                // iss parameter is not allowed when JARM is used
                () -> assertFalse(responseParameters.containsKey("iss"))
        );
    }

}
