package no.idporten.eudiw.issuer.openid4vci;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialConfigurationService;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.credentials.CredentialCreateService;
import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.credentials.types.StringValue;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
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

import java.net.URI;
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
    private CredentialCreateService credentialCreateService;

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
            ClaimsSource claimsSource = mock(ClaimsSource.class);
            CredentialConfigurationProperties credentialConfigurationProperties = CredentialConfigurationProperties.builder()
                    .authorizationServer("https://junit.idporten.no")
                    .scope("foo:bar")
                    .format(CredentialFormat.SD_JWT_VC)
                    .credentialType("foodoc")
                    .claimsSourceUri(URI.create("class://" + claimsSource.getClass().getName()))
                    .build();
            List<Claim> claims = List.of(Claim.builder().path("n1").path("p1").value(new StringValue("v1")).build());
            when(claimsSource.issueClaims(credentialIssueContextCaptor.capture())).thenReturn(claims);
            when(credentialConfigurationService.findCredentialConfiguration(eq("cid"))).thenReturn(credentialConfigurationProperties);
            when(claimsSourceService.findClaimsSource(any(URI.class))).thenReturn(claimsSource);
            when(credentialCreateService.createCredentials(isNull(), eq(credentialConfigurationProperties), anyList()))
                    .thenReturn(List.of(Credential.builder().credential("{credential-with-n1-p1-v1}").build()));
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
            verify(auditService).logIssueCredentials(eq("https://junit.idporten.no"), eq("cid"), eq(issuanceTransactionId), eq(CredentialFormat.SD_JWT_VC), eq(new NotificationId("nid")) ,eq(accessToken));
        }

    }

}
