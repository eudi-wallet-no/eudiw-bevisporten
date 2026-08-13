package no.idporten.eudiw.login.oidc.api;


import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationResponse;
import no.idporten.sdk.oidcserver.protocol.TokenRequest;
import no.idporten.sdk.oidcserver.protocol.TokenResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("junit")
@AutoConfigureMockMvc
@SpringBootTest
public class OIDCServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private OpenIDConnectIntegration openIDConnectServer;

    @DisplayName("When using OIDC discovery to explore metadata")
    @Nested
    class MetadataEndpointTests {

        @DisplayName("then metadata can also be found via the application root")
        @Test
        void rootRedirectsToMetadata() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/.well-known/openid-configuration"));
        }

        @DisplayName("then metadata contains acr values for EU and Norwegian logins")
        @Test
        void metadataContainsAcrValuesForEUAndNorwegianLogins() throws Exception {
            mockMvc.perform(get("/.well-known/openid-configuration"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.acr_values_supported", Matchers.hasItems(
                            "idporten-loa-substantial",
                            "idporten-loa-high",
                            "eidas-loa-low",
                            "eidas-loa-substantial",
                            "eidas-loa-high")))
                    .andExpect(jsonPath("$.acr_values_supported", Matchers.hasSize(5)));
        }
    }

    @DisplayName("When using the jwks endpoint")
    @Nested
    class JWKSEndpointTests {

        @DisplayName("then the json web key set contains a single RSA public key for token signing")
        @Test
        void jsonWebKeySetContainsOneKey() throws Exception {
            MvcResult result = mockMvc.perform(get("/jwks"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.keys", Matchers.hasSize(1)))
                    .andReturn();
            JWKSet jwkSet = JWKSet.parse(result.getResponse().getContentAsString());
            JWK jwk = jwkSet.getKeys().get(0);
            assertAll(
                    () -> assertEquals("RSA", jwk.getKeyType().getValue()),
                    () -> assertEquals("sig", jwk.getKeyUse().identifier()),
                    () -> assertFalse(jwk.isPrivate())
            );
        }
    }

    @DisplayName("When using the pushed authorization request (par) endpoint")
    @Nested
    class PushedAuthorizationRequestEndpointTests {

        @DisplayName("then a valid request returns a pushed authorization response with a request URI")
        @Test
        void validPushedAuthorizationRequest() throws Exception {
            doReturn(
                    PushedAuthorizationResponse.builder()
                            .requestUri("https://junit.eidas2sandkasse.net/authorize")
                            .build())
                    .when(openIDConnectServer)
                    .process(any(PushedAuthorizationRequest.class));
            mockMvc.perform(post("/par")
                            .contentType("application/x-www-form-urlencoded")
                            .accept("application/json"))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.request_uri").value("https://junit.eidas2sandkasse.net/authorize"));
        }

        @DisplayName("then an invalid request returns a error response")
        @Test
        void invalidPushedAuthorizationRequest() throws Exception {
            mockMvc.perform(post("/par")
                            .contentType("application/x-www-form-urlencoded")
                            .accept("application/json"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.error").value("invalid_client"));
        }
    }

    @DisplayName("When using the token endpoint")
    @Nested
    class TokenEndpointTests {

        @DisplayName("then a valid request returns a token response")
        @Test
        void validTokenRequest() throws Exception {
            doReturn(
                    TokenResponse.builder()
                            .accessToken("at")
                            .idToken("it")
                            .build())
                    .when(openIDConnectServer)
                    .process(any(TokenRequest.class));
            mockMvc.perform(post("/token")
                            .contentType("application/x-www-form-urlencoded")
                            .accept("application/json"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.access_token").value("at"));
        }

        @DisplayName("then an invalid request returns a error response")
        @Test
        void invalidTokenRequest() throws Exception {
            mockMvc.perform(post("/token")
                            .contentType("application/x-www-form-urlencoded")
                            .accept("application/json"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.error").value("invalid_client"));
        }
    }

}