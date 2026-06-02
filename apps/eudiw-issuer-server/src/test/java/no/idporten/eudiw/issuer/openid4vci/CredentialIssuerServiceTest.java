package no.idporten.eudiw.issuer.openid4vci;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.authoritativesources.JUnitClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.CredentialCreateService;
import no.idporten.eudiw.issuer.credentials.configurations.CredentialIssuerContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.credentials.types.StringValue;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.metrics.MetricService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenCredentialValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import no.idporten.eudiw.issuer.openid4vci.proofs.ProofService;
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
    private ClaimsSourceService claimsSourceService;

    @Mock
    private AccessTokenValidationService accessTokenValidationService;

    @Mock
    private ProofService proofService;

    @Mock
    private CredentialIssuanceStatusService credentialIssuanceStatusService;

    @Mock
    private CredentialCreateService credentialCreateService;

    @Mock
    private AuditService auditService;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private CredentialIssuerService credentialIssuerService;

    @Captor
    private ArgumentCaptor<AccessTokenCredentialValidationContext> accessTokenCredentialValidationContextCaptor;


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
            ClaimsSource claimsSource = mock(JUnitClaimsSource.class);
            ExtendedCredentialConfiguration credentialConfiguration = ExtendedCredentialConfiguration.builder()
                    .scope("foo:bar")
                    .format(CredentialFormat.SD_JWT_VC)
                    .credentialType("foodoc")
                    .credentialIssuerContext(CredentialIssuerContext.builder()
                            .authorizationServer("junit")
                            .credentialDataSourceUri(URI.create("class://" + claimsSource.getClass().getName()))
                            .build())
                    .build();
            CredentialIssuerTenant credentialIssuerTenant = spy(CredentialIssuerTenant.builder().credentialIssuer(URI.create("https://junit.idporten.no")).build());
            doReturn(credentialConfiguration).when(credentialIssuerTenant).findCredentialConfiguration(eq("cid"));

            List<Claim> claims = List.of(Claim.builder().path("n1").path("p1").value(new StringValue("v1")).build());
            when(claimsSource.issueClaims(credentialIssueContextCaptor.capture())).thenReturn(claims);
            when(claimsSourceService.findClaimsSource(any(URI.class))).thenReturn(claimsSource);
            when(credentialCreateService.createCredentials(any(), anyList(), anyList()))
                    .thenReturn(List.of(Credential.builder().credential("{credential-with-n1-p1-v1}").build()));
            IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId(transactionId);
            when(credentialIssuanceStatusService.credentialIssued(eq("cid"), eq(issuanceTransactionId))).thenReturn(new NotificationId("nid"));
            CredentialResponse credentialResponse = credentialIssuerService.issueCredentials(credentialIssuerTenant, credentialRequest, accessToken);
            List<Credential> credentials = credentialResponse.getCredentials();
            assertAll(
                    () -> assertEquals(accessToken, credentialIssueContextCaptor.getValue().accessToken()),
                    () -> assertEquals(1, credentials.size()),
                    () -> assertTrue(credentials.getFirst().getCredential().contains("n1")),
                    () -> assertTrue(credentials.getFirst().getCredential().contains("p1")),
                    () -> assertTrue(credentials.getFirst().getCredential().contains("v1")),
                    () -> assertEquals("nid", credentialResponse.getNotificationId().getValue())
            );
            verify(accessTokenValidationService).validateAccessTokenForCredentialConfiguration(eq(accessToken), accessTokenCredentialValidationContextCaptor.capture());
            assertAll(
                    () -> assertEquals("junit", accessTokenCredentialValidationContextCaptor.getValue().authorizationServer()),
                    () -> assertEquals("foo:bar", accessTokenCredentialValidationContextCaptor.getValue().scope())
            );
            verify(credentialIssuanceStatusService).credentialIssued(eq("cid"), any(IssuanceTransactionId.class));
            verify(auditService).logIssueCredentials(eq("junit"), eq("cid"), eq(issuanceTransactionId), eq(CredentialFormat.SD_JWT_VC), eq(1), eq(new NotificationId("nid")) ,eq(accessToken));
        }

    }

}
