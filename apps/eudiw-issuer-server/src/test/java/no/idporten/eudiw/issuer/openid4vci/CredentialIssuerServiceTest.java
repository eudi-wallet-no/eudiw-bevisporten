package no.idporten.eudiw.issuer.openid4vci;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.StringValue;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialConfigurationService;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When issuing credentials")
@ExtendWith(MockitoExtension.class)
public class CredentialIssuerServiceTest {

    @Mock
    private CredentialIssuerServerProperties credentialIssuerServerProperties;

    @Mock
    private CredentialConfigurationService credentialConfigurationService;

    @Mock
    private ClaimsSourceService claimsSourceService;

    @Mock
    private AccessTokenValidationService accessTokenValidationService;

    @Mock
    private CredentialIssuanceStatusService credentialIssuanceStatusService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CredentialIssuerService credentialIssuerService;

    @DisplayName("When issuing credentials")
    @Nested
    class IssueTests {

        @Captor
        private ArgumentCaptor<CredentialIssueContext> credentialIssueContextCaptor;

        @DisplayName("then a valid credential request with access_token from valid authorization with valid scope will invoke the correct claims source")
        @Test
        void testInvokeClaimsSource() {
            String transactionId = "111";
            JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                    .issuer("https://junit.idporten.no")
                    .claim("scope", "openid profile foo:bar")
                    .claim("tx_id", transactionId)
                    .build();
            PlainJWT accessToken = new PlainJWT(jwtClaimsSet);
            CredentialRequest credentialRequest = CredentialRequest.builder().credentialConfigurationId("cid").build();
            CredentialConfigurationProperties credentialConfigurationProperties = CredentialConfigurationProperties.builder()
                    .authorizationServer("https://junit.idporten.no")
                    .scope("foo:bar")
                    .format(CredentialFormat.JSON_DEBUG)
                    .credentialType("foodoc")
                    .build();
            ClaimsSource claimsSource = mock(ClaimsSource.class);
            when(claimsSource.issueClaims(credentialIssueContextCaptor.capture())).thenReturn(List.of(Claim.builder().path("n1").path("p1").value(new StringValue("v1")).build()));
            when(credentialConfigurationService.findCredentialConfiguration(eq("cid"))).thenReturn(credentialConfigurationProperties);
            when(claimsSourceService.findClaimsSource(eq("foodoc"))).thenReturn(claimsSource);
            IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId(transactionId);
            when(credentialIssuanceStatusService.credentialIssued(eq("cid"), eq(issuanceTransactionId))).thenReturn(new NotificationId("nid"));
            CredentialResponse credentialResponse = credentialIssuerService.issueCredentials(credentialRequest, accessToken);
            List<Credential> credentials = credentialResponse.getCredentials();
            assertAll(
                    () -> assertEquals(accessToken, credentialIssueContextCaptor.getValue().accessToken()),
                    () -> assertEquals(1, credentials.size()),
                    () -> assertTrue(credentials.getFirst().getCredential().contains("n1")),
                    () -> assertTrue(credentials.getFirst().getCredential().contains("p1")),
                    () -> assertTrue(credentials.getFirst().getCredential().contains("v1")),
                    () -> assertEquals("nid", credentialResponse.getNotificationId().getValue())
            );
            verify(accessTokenValidationService).validateAccessTokenForCredentialConfiguration(eq(accessToken), eq("https://junit.idporten.no"), eq("foo:bar"));
            verify(credentialIssuanceStatusService).credentialIssued(eq("cid"), any(IssuanceTransactionId.class));
            verify(auditService).logIssueCredentials(eq("https://junit.idporten.no"), eq("cid"), eq(issuanceTransactionId), eq(CredentialFormat.JSON_DEBUG), eq(new NotificationId("nid")) ,eq(accessToken));
        }

    }

}
