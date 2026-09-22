package no.idporten.eudiw.verifier.api.verification;

import no.idporten.eudiw.verifier.openid4vp.VerificationService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("When using the verification API")
@ActiveProfiles("junit")
@AutoConfigureMockMvc
@SpringBootTest
class VerificationControllerTest {

    private static final String CLIENT_APPLICATION_ID = "junit";
    private static final String VERIFIER_TRANSACTION_ID = "transaction-id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationService verificationService;

    @Test
    @DisplayName("starts a verification")
    void startsVerificationWithoutValidationDetails() throws Exception {
        when(verificationService.startVerification(any(), any(), eq(false)))
                .thenReturn(new StartVerificationResponse(
                        URI.create("eudi-openid4vp://authorize"),
                        URI.create("data:image/png;base64,abc"),
                        VERIFIER_TRANSACTION_ID));

        mockMvc.perform(post("/api/v1/{clientApplicationId}/verify/start/", CLIENT_APPLICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dcql_query": { "credentials": [] },
                                  "redirect_uri": "https://example.test/verification-complete"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifier_transaction_id").value(VERIFIER_TRANSACTION_ID));

        verify(verificationService).startVerification(any(), any(), eq(false));
    }

    @Test
    @DisplayName("starts a verification with validation details requested")
    void startsVerification() throws Exception {
        when(verificationService.startVerification(any(), any(), eq(true)))
                .thenReturn(new StartVerificationResponse(
                        URI.create("eudi-openid4vp://authorize"),
                        URI.create("data:image/png;base64,abc"),
                        VERIFIER_TRANSACTION_ID));

        mockMvc.perform(post("/api/v1/{clientApplicationId}/verify/start/", CLIENT_APPLICATION_ID)
                        .queryParam("include_validation_details", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dcql_query": { "credentials": [] },
                                  "redirect_uri": "https://example.test/verification-complete"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifier_transaction_id").value(VERIFIER_TRANSACTION_ID));

        verify(verificationService).startVerification(any(), any(), eq(true));
    }


    @Test
    @DisplayName("returns the verification status")
    void returnsVerificationStatus() throws Exception {
        when(verificationService.verifierStatus(any(), any()))
                .thenReturn(new VerificationStatusResponse("ERROR", VERIFIER_TRANSACTION_ID));

        mockMvc.perform(get("/api/v1/{clientApplicationId}/verify/status/{transactionId}",
                        CLIENT_APPLICATION_ID, VERIFIER_TRANSACTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.verifier_transaction_id").value(VERIFIER_TRANSACTION_ID));

        verify(verificationService).verifierStatus(eq(VERIFIER_TRANSACTION_ID), any());
    }

    @Test
    @DisplayName("returns the verification result")
    void returnsVerificationResult() throws Exception {
        when(verificationService.retrieveVerificationData(any(), any()))
                .thenReturn(new VerificationResultResponse(VERIFIER_TRANSACTION_ID, null, null, null));

        mockMvc.perform(get("/api/v1/{clientApplicationId}/verify/result/{transactionId}",
                        CLIENT_APPLICATION_ID, VERIFIER_TRANSACTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifier_transaction_id").value(VERIFIER_TRANSACTION_ID));

        verify(verificationService).retrieveVerificationData(eq(VERIFIER_TRANSACTION_ID), any());
    }
}
