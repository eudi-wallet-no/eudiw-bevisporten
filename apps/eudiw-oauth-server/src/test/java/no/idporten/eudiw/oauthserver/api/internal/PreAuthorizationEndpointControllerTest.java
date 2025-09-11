package no.idporten.eudiw.oauthserver.api.internal;

import no.idporten.sdk.oidcserver.cache.OpenIDConnectCache;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When uploading pre-authorizations")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class PreAuthorizationEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenIDConnectCache cache;

    @Captor
    ArgumentCaptor<String> preAuthorizedCodeCaptor;

    @Captor
    ArgumentCaptor<Authorization> authorizationCaptor;

    @DisplayName("then requests without API key are rejected")
    @Test
    void testMissingApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/pre-authorizations")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                    "who": "cares"
                                 }"""))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value(Matchers.containsString("Missing API key header")));
    }

    @DisplayName("then requests with invalid API key are rejected")
    @Test
    void testInvalidApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/pre-authorizations")
                        .header("X-API-KEY", "gfdjhgfjhadf")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                    "who": "cares"
                                 }"""))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value(Matchers.containsString("Invalid API key")));
    }

    @DisplayName("then requests with invalid pre-authorization attributes is rejected")
    @Test
    void testInvalidPreAuthorizationAttributes() throws Exception {
        mockMvc.perform(post("/api/v1/pre-authorizations")
                        .header("X-API-KEY", "junit")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                    "who": "cares"
                                 }"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @DisplayName("then received data is stored in cache and a pre-authorized code is returned")
    @Test
    void testUploadAuthorizationAndReceivePreAuthorizedCode() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pre-authorizations")
                        .header("X-API-KEY", "junit")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                    "sub": "12345678901",
                                    "aud": "foo",
                                    "scope": ["scp1", "scp2"],
                                    "tx_id": "tid",
                                    "unknown": "ignored"
                                 }"""))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.pre-authorized_code").isNotEmpty())
                .andExpect(jsonPath("$.expires_in").isNumber())
                .andReturn();
        verify(cache).putAuthorization(preAuthorizedCodeCaptor.capture(), authorizationCaptor.capture());
        Authorization authorization = authorizationCaptor.getValue();
        assertAll(
                () -> assertTrue(result.getResponse().getContentAsString().contains(preAuthorizedCodeCaptor.getValue())),
                () -> assertEquals("12345678901", authorization.getSub()),
                () -> assertEquals("foo", authorization.getAud()),
                () -> assertEquals("scp1 scp2", authorization.getScope()),
                () -> assertEquals("tid", authorization.getAttributes().get("tx_id"))
        );
    }

}
