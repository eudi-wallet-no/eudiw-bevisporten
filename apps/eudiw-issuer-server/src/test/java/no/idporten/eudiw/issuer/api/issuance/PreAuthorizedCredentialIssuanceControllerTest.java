package no.idporten.eudiw.issuer.api.issuance;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.issuance.preauth.PreAuthorizedIssuanceService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import no.idporten.eudiw.issuer.issuance.preauth.PreAuthorizedIssuanceResponse;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("When using the start credential issuance endpoint")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class PreAuthorizedCredentialIssuanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    PreAuthorizedIssuanceService preAuthorizedIssuanceService;

    @MockitoSpyBean
    AccessTokenValidationService accessTokenValidationService;

    protected JWT accessToken(String personIdentifier) {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer("https://junit.idporten.no")
                .claim("scope", "openid profile foo:bar")
                .claim("pid", personIdentifier)
                .build();
        return new PlainJWT(jwtClaimsSet);
    }

    @DisplayName("then the subject identifier must match the pid claim in the access token")
    @Test
    void testInvalidSubjectIdentifier() throws Exception {
        JWT accessToken = accessToken("16903349844");
        doReturn(accessToken).when(accessTokenValidationService).validateAccessToken(any());
        mockMvc.perform(post("/api/v1/credential/issuance-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken.serialize())
                        .content("""
                                {
                                  "credential_configuration_id": "any",
                                  "subject": {
                                    "identifier": "99999999999"
                                  }
                                }"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.error_description").value(Matchers.containsString("subject/person identifier does not match")));
    }

    @DisplayName("then a valid request starts credential issuance")
    @Test
    void testValidRequestResponse() throws Exception {
        JWT accessToken = accessToken("16903349844");
        doReturn(accessToken).when(accessTokenValidationService).validateAccessToken(any());
        when(preAuthorizedIssuanceService.startIssuerTransaction(any(), eq(accessToken))).thenReturn(PreAuthorizedIssuanceResponse
                .builder()
                .credentialOffer(CredentialOffer.builder().build())
                .issuanceTransactionId(new IssuanceTransactionId("txid"))
                .build());
        mockMvc.perform(post("/api/v1/credential/issuance-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken.serialize())
                        .content("""
                                {
                                  "credential_configuration_id": "any",
                                  "subject": {
                                    "identifier": "16903349844"
                                  }
                                }"""))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.issuance_transaction_id").value("txid"))
                .andExpect(jsonPath("$.credential_offer").exists());
    }

}
