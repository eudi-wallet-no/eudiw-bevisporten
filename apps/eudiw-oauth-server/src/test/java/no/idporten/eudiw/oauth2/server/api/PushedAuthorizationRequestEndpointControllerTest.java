package no.idporten.eudiw.oauth2.server.api;

import no.idporten.eudiw.oauth2.server.OpenID4VCIAuthorizationServer;
import no.idporten.eudiw.oauth2.server.UseAttestationChallengeOAuth2Exception;
import no.idporten.eudiw.oauth2.server.protocol.ChallengeRequest;
import no.idporten.eudiw.oauth2.server.protocol.ChallengeResponse;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationRequest;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationResponse;
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

@DisplayName("When calling the Pushed Authorization Request endpoint")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
class PushedAuthorizationRequestEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenID4VCIAuthorizationServer authorizationServer;

    @DisplayName("then challenge header is included in response")
    @Test
    void testChallengeHeaderIncludedInResponse() throws Exception {
        PushedAuthorizationResponse parResponse = PushedAuthorizationResponse.builder()
                .expiresIn(600)
                .requestUri("urn:example:par:request:uri")
                .httpStatusCode(201)
                .build();

        ChallengeResponse challengeResponse = ChallengeResponse.builder()
                .attestationChallenge("test-challenge-value")
                .build();

        when(authorizationServer.process(any(PushedAuthorizationRequest.class)))
                .thenReturn(parResponse);
        when(authorizationServer.process(any(ChallengeRequest.class)))
                .thenReturn(challengeResponse);

        mockMvc.perform(post("/par")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("client_id", "test-client"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(UseAttestationChallengeOAuth2Exception.OAUTH_CLIENT_ATTESTATION_CHALLENGE_HEADER))
                .andExpect(header().string(UseAttestationChallengeOAuth2Exception.OAUTH_CLIENT_ATTESTATION_CHALLENGE_HEADER, "test-challenge-value"));
    }
}


