package no.idporten.eudiw.oauth2.server.api;

import no.idporten.eudiw.oauth2.server.OpenID4VCIAuthorizationServer;
import no.idporten.eudiw.oauth2.server.UseAttestationChallengeOAuth2Exception;
import no.idporten.eudiw.oauth2.server.protocol.ChallengeRequest;
import no.idporten.eudiw.oauth2.server.protocol.ChallengeResponse;
import no.idporten.eudiw.oauth2.server.protocol.TokenRequest;
import no.idporten.eudiw.oauth2.server.protocol.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When calling the Token endpoint")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
class TokenEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenID4VCIAuthorizationServer authorizationServer;

    @DisplayName("then challenge header is included in response")
    @Test
    void testChallengeHeaderIncludedInResponse() throws Exception {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("test-access-token")
                .tokenType("Bearer")
                .expiresInSeconds(3600)
                .build();

        ChallengeResponse challengeResponse = ChallengeResponse.builder()
                .attestationChallenge("next-challenge-value")
                .build();

        when(authorizationServer.process(any(TokenRequest.class)))
                .thenReturn(tokenResponse);
        when(authorizationServer.process(any(ChallengeRequest.class)))
                .thenReturn(challengeResponse);

        mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("grant_type", "authorization_code")
                        .param("code", "test-code")
                        .param("client_id", "test-client"))
                .andExpect(status().isOk())
                .andExpect(header().exists(UseAttestationChallengeOAuth2Exception.OAUTH_CLIENT_ATTESTATION_CHALLENGE_HEADER))
                .andExpect(header().string(UseAttestationChallengeOAuth2Exception.OAUTH_CLIENT_ATTESTATION_CHALLENGE_HEADER, "next-challenge-value"));
    }
}


