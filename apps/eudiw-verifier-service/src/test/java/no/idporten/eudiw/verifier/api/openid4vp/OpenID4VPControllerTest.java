package no.idporten.eudiw.verifier.api.openid4vp;

import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.openid.connect.sdk.Nonce;
import jakarta.servlet.http.HttpSession;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.OpenID4VPRequestService;
import no.idporten.eudiw.verifier.openid4vp.OpenID4VPResponseService;
import no.idporten.eudiw.verifier.openid4vp.SessionRecordElements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When using the OpenID4VP API")
@ActiveProfiles("junit")
@AutoConfigureMockMvc
@SpringBootTest
class OpenID4VPControllerTest {

    private static final String CLIENT_APPLICATION_ID = "junit";
    private static final String REQUEST_ID = "request-id";
    private static final String VERIFIER_TRANSACTION_ID = "transaction-id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenID4VPRequestService openID4VPRequestService;

    @MockitoBean
    private OpenID4VPResponseService openID4VPResponseService;

    private MockHttpSession mockHttpSession = new MockHttpSession();

    @BeforeEach
    void setUp() throws Exception {
        mockHttpSession.setAttribute("sessionRecordElements", new SessionRecordElements( new Nonce("nonce"), new Audience("http://example.com/")));
    }


    @Test
    @DisplayName("returns an authorization request using the default flow")
    void returnsAuthorizationRequestUsingDefaultFlow() throws Exception {
        when(openID4VPRequestService.retrieveAuthorizationRequest(any(MockHttpSession.class), any(), eq(REQUEST_ID), eq("same_device")))
                .thenReturn("signed-authorization-request");

        mockMvc.perform(get("/openid4vp/authz-request/{clientApplicationId}/{requestId}",
                        CLIENT_APPLICATION_ID, REQUEST_ID)
                        .session(mockHttpSession))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/oauth-authz-req+jwt")))
                .andExpect(content().string("signed-authorization-request"));

        verify(openID4VPRequestService).retrieveAuthorizationRequest(
                any(HttpSession.class),
                argThat(client -> CLIENT_APPLICATION_ID.equals(client.getId())),
                eq(REQUEST_ID),
                eq("same_device"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"same_device", "cross_device"})
    @DisplayName("returns an authorization request using the requested flow")
    void returnsAuthorizationRequestUsingRequestedFlow(String flow) throws Exception {
        when(openID4VPRequestService.retrieveAuthorizationRequest(any(HttpSession.class), any(), eq(REQUEST_ID), eq(flow)))
                .thenReturn("signed-authorization-request");

        mockMvc.perform(get("/openid4vp/authz-request/{clientApplicationId}/{requestId}",
                        CLIENT_APPLICATION_ID, REQUEST_ID)
                        .session(mockHttpSession)
                        .queryParam("flow", flow))
                .andExpect(status().isOk())
                .andExpect(content().string("signed-authorization-request"));

        verify(openID4VPRequestService).retrieveAuthorizationRequest(
                any(HttpSession.class),
                any(ClientApplication.class),
                eq(REQUEST_ID),
                eq(flow));
    }

    @Test
    @DisplayName("lets service handle authorization responses")
    void receivesAuthorizationResponse() throws Exception {
        WalletCallback walletCallback = new WalletCallback();
        walletCallback.setRedirectUri(URI.create("https://example.test/verification-complete"));
        when(openID4VPResponseService.receiveResponse(any(HttpSession.class), any(), eq(VERIFIER_TRANSACTION_ID), any())).thenReturn(walletCallback);

        mockMvc.perform(post("/openid4vp/authz-response/{clientApplicationId}/{transactionId}",
                        CLIENT_APPLICATION_ID, VERIFIER_TRANSACTION_ID)
                        .session(mockHttpSession)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("response", "encrypted-authorization-response"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri")
                        .value("https://example.test/verification-complete"));
    }

}
