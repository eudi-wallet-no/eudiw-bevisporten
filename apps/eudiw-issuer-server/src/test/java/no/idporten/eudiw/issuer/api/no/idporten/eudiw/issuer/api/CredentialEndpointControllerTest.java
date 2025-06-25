package no.idporten.eudiw.issuer.api.no.idporten.eudiw.issuer.api;


import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.CredentialIssuerService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidator;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When using the credential endpoint")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class CredentialEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorizationServerService authorizationServerService;

    @MockitoBean
    private CredentialIssuerService credentialIssuerService;


    private String sampleBearerToken() {
        return "eyJraWQiOiIyUEhBMTVBM1UzMkFBMFJaOEp2ZGhqVUwxYjVOckV1eHlCNVVibGtMdk9nIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIxNTgxNTg5ODk0NyIsImNsbSI6WyJhZ2Vfb3Zlcl8xOCIsImJpcnRoX2RhdGUiLCJiaXJ0aF9wbGFjZSIsInBpZF9leHBpcnlfZGF0ZSIsImdpdmVuX25hbWUiLCJuYXRpb25hbGl0eSIsImlzc3VpbmdfY291bnRyeSIsImlzc3VpbmdfYXV0aG9yaXR5IiwiZmFtaWx5X25hbWUiXSwiaXNzIjoiaHR0cHM6Ly9pZHBvcnRlbi5kZXYiLCJjbGllbnRfYW1yIjoiY2xpZW50X3NlY3JldF9iYXNpYyIsInBpZCI6IjE1ODE1ODk4OTQ3IiwiY2xpZW50X2lkIjoiZGVtb2NsaWVudF9pZHBvcnRlbl9zeXN0ZXN0IiwiYWNyIjoiaWRwb3J0ZW4tbG9hLXN1YnN0YW50aWFsIiwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBub2JpZDpwaWQiLCJleHAiOjE3NDc3NDU0MzEsImlhdCI6MTc0Nzc0NTMxMSwianRpIjoiYmNxNHJyVVM1c1kiLCJjb25zdW1lciI6eyJhdXRob3JpdHkiOiJpc282NTIzLWFjdG9yaWQtdXBpcyIsIklEIjoiMDE5Mjo5OTE4MjU4MjcifX0.rBA7QuJiCj__oMHIMp1zVDJ_lDqkaewEJk0iTOrEEehL7j4waCgprCNTB4NBMmi0Wya2Bl5DvAbSwsSQDI5wSRWnTEWoy9-CPwIPBDUvkhSoVG_8gvNwzFI8_Dh8iKzXzQiAOdYRRiQsLS-hcoqs0vb6VCf-kf_PHvhxwAKfA-ocPUangCMLUzOeJLjNA4o-ybIj0ZkOrwR0n8lJ3-AA2Bt6t4UDVcvdLgkP5VLj_AHL6ceoc4jE8e8yGM76hMFcfz0rOKOWGA5yZH2s08Z3ocW5Q9LDdXqQfizQ5lGFgjC4qJ72e1F_W-aBnuKchkFzgfTDk_n450IbGhjpjprrZHP2_33mwlauD2kZw8-OBGTOk41d5VdRxUkii-g67QcYYvWgqRwbZGQCejIEtSrkjPkyhMqQSWFk4WZU0LI0KE0JvA4e3AYo1A8rtDilRed-HeHNTHhD25QCO_pYvkszR2CPUq9LxlgD2FSzmp6GuQT-dpS9R0VQvtgSOQzjwQsNsDt18sN-9LEJxcRRGf5BV2asMfZuuxzckvkLR_cOX1FoscPdKvuxxEgZqD0q1neLljGr6-5LtX_ipHSExqQ2-HyPqUtF6d3toPlLTdpVqw3i_SehBdXW2gnoXsraI1KTY7IZwQOjeclqp9m937m96LSGQ2RRX4jnkAHunJwXUl0";
    }

    private void setUpAccessTokenValidation() {
        AccessTokenValidator accessTokenValidator = mock(AccessTokenValidator.class);
        when(accessTokenValidator.validate(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        AuthorizationServer authorizationServer = mock(AuthorizationServer.class);
        when(authorizationServer.getAccessTokenValidator()).thenReturn(accessTokenValidator);
        when(authorizationServerService.findAuthorizationServer(any())).thenReturn(authorizationServer);
    }

    @DisplayName("then a valid credential request gives a credential response")
    @Test
    void testPostCredentialRequest() throws Exception {
        setUpAccessTokenValidation();
        when(credentialIssuerService.issueCredentials(any(CredentialRequest.class), any(JWT.class))).thenReturn(List.of(Credential.builder().credential("foo").build()));
        mockMvc.perform(post("/openid4vci/credential")
                        .header("Authorization", "Bearer %s".formatted(sampleBearerToken()))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                     "credential_configuration_id": "no.digdir.eudiw.pid_mso_mdoc",
                                     "proof": {
                                         "jwt": "eyJhbGciOiJFUzI1NiIsImp3ayI6eyJhbGciOiJFUzI1NiIsImNydiI6IlAtMjU2Iiwia2lkIjoiMUUxQzUzQTYtOTNCMS00MTdFLUFBMzQtQjE0RTMyNjNEMTRBIiwia3R5IjoiRUMiLCJ1c2UiOiJzaWciLCJ4IjoiV1lGSGwwVTNBMXRCZHA4Y09IdS1YRGh6aEpjcF9XNkhJOWkyVU81UTUycyIsInkiOiJSLWZFdUhvWWVhMWNBTXJtSF9rTVNnWEhhb3NPSE9tc1QyVEh5VzlCU1lFIn0sInR5cCI6Im9wZW5pZDR2Y2ktcHJvb2Yrand0In0.eyJhdWQiOiJodHRwczpcL1wvZGVtby11dHN0ZWRlci50ZXN0LmVpZGFzMnNhbmRrYXNzZS5uZXQiLCJpYXQiOjE3NTAzMjc1MTksImlzcyI6ImRlbW8tbG9tbWVib2sifQ.-LSGbLPF3NDQoFEZInkhIDqLVcu7n5Zp-_5cQo3wYlSNSogwUa1FVmg6WOnKr6zHtb4v2ipRdnYjS2tq2LbXIg",
                                         "type": "jwt"
                                     }
                                 }"""))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.credentials").isArray())
                .andExpect(jsonPath("$.credentials").isNotEmpty())
                .andExpect(jsonPath("$.credentials[0].credential").value("foo"));
    }

    @DisplayName("then an authorization header with bearer token is required")
    @Test
    void testInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/openid4vci/credential")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bjørnar at")
                        .content(" {}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_request"));
        verifyNoInteractions(credentialIssuerService);
    }

    @DisplayName("then an empty credential request gives a credential error response")
    @Test
    void testEmptyCredentialRequest() throws Exception {
        setUpAccessTokenValidation();
        mockMvc.perform(post("/openid4vci/credential")
                        .header("Authorization", "Bearer %s".formatted(sampleBearerToken()))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(" {}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_credential_request"));
        verifyNoInteractions(credentialIssuerService);
    }

    @DisplayName("then a credential request can not have both credential_identifier and credential_configuration_id")
    @Test
    void testEitherCredentialIdentifierOrCredentialConfigurationId() throws Exception {
        setUpAccessTokenValidation();
        mockMvc.perform(post("/openid4vci/credential")
                        .header("Authorization", "Bearer %s".formatted(sampleBearerToken()))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                    "credential_identifier": "ci",
                                    "credential_configuration_id": "cci"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_credential_request"));
        verifyNoInteractions(credentialIssuerService);
    }

    @DisplayName("then a credential without proof gives credential error response with a c_nonce")
    @Test
    void testMissingProofCredentialRequest() throws Exception {
        setUpAccessTokenValidation();
        when(credentialIssuerService.issueCredentials(any(CredentialRequest.class), any(JWT.class))).thenReturn(List.of(Credential.builder().credential("foo").build()));
        mockMvc.perform(post("/openid4vci/credential")
                        .header("Authorization", "Bearer %s".formatted(sampleBearerToken()))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                     "credential_configuration_id": "no.digdir.eudiw.pid_mso_mdoc"
                                 }"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_proof"))
                .andExpect(jsonPath("$.c_nonce").isNotEmpty());
    }

}
