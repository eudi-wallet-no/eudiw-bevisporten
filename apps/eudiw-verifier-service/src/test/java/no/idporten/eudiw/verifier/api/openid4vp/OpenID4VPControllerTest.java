package no.idporten.eudiw.verifier.api.openid4vp;

import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.OpenID4VPRequestService;
import no.idporten.eudiw.verifier.openid4vp.OpenID4VPResponseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    @DisplayName("returns an authorization request using the default flow")
    void returnsAuthorizationRequestUsingDefaultFlow() throws Exception {
        when(openID4VPRequestService.retrieveAuthorizationRequest(any(), eq(REQUEST_ID), eq("same_device")))
                .thenReturn("signed-authorization-request");

        mockMvc.perform(get("/openid4vp/authz-request/{clientApplicationId}/{requestId}",
                        CLIENT_APPLICATION_ID, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/oauth-authz-req+jwt")))
                .andExpect(content().string("signed-authorization-request"));

        verify(openID4VPRequestService).retrieveAuthorizationRequest(
                argThat(client -> CLIENT_APPLICATION_ID.equals(client.getId())),
                eq(REQUEST_ID),
                eq("same_device"));
    }

    @Test
    @DisplayName("returns an authorization request using the requested flow")
    void returnsAuthorizationRequestUsingRequestedFlow() throws Exception {
        when(openID4VPRequestService.retrieveAuthorizationRequest(any(), eq(REQUEST_ID), eq("cross_device")))
                .thenReturn("signed-authorization-request");

        mockMvc.perform(get("/openid4vp/authz-request/{clientApplicationId}/{requestId}",
                        CLIENT_APPLICATION_ID, REQUEST_ID)
                        .queryParam("flow", "cross_device"))
                .andExpect(status().isOk())
                .andExpect(content().string("signed-authorization-request"));

        verify(openID4VPRequestService).retrieveAuthorizationRequest(
                any(ClientApplication.class),
                eq(REQUEST_ID),
                eq("cross_device"));
    }

    @Test
    @DisplayName("receives an authorization response")
    void receivesAuthorizationResponse() throws Exception {
        WalletCallback walletCallback = new WalletCallback();
        walletCallback.setRedirectUri(URI.create("https://example.test/verification-complete"));
        when(openID4VPResponseService.receiveResponse(any(), eq(VERIFIER_TRANSACTION_ID), any()))
                .thenReturn(walletCallback);

        mockMvc.perform(post("/openid4vp/authz-response/{clientApplicationId}/{transactionId}",
                        CLIENT_APPLICATION_ID, VERIFIER_TRANSACTION_ID)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("response", "encrypted-authorization-response"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri")
                        .value("https://example.test/verification-complete"));

        verify(openID4VPResponseService).receiveResponse(
                argThat(client -> CLIENT_APPLICATION_ID.equals(client.getId())),
                eq(VERIFIER_TRANSACTION_ID),
                argThat(response -> "encrypted-authorization-response".equals(response.getResponse())));
    }

    @Test
    @DisplayName("rejects a missing authorization response")
    void rejectsMissingAuthorizationResponse() throws Exception {
        mockMvc.perform(post("/openid4vp/authz-response/{clientApplicationId}/{transactionId}",
                        CLIENT_APPLICATION_ID, VERIFIER_TRANSACTION_ID)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("Missing authorization response"));

        verifyNoInteractions(openID4VPResponseService);
    }
}
