package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.StringValue;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;
import no.idporten.eudiw.issuer.openid4vci.protocol.*;
import no.idporten.eudiw.issuer.openid4vci.service.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.service.NotificationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
            when(claimsSource.retrieveClaims(eq(accessToken))).thenReturn(List.of(Claim.builder().path("n1").path("p1").value(new StringValue("v1")).build()));
            when(credentialIssuerServerProperties.findCredentialConfiguration(eq("cid"))).thenReturn(credentialConfigurationProperties);
            when(claimsSourceService.findClaimsSource(eq("foodoc"))).thenReturn(claimsSource);
            IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId(transactionId);
            when(credentialIssuanceStatusService.credentialIssued(eq("cid"), eq(issuanceTransactionId))).thenReturn(new NotificationId("nid"));


            CredentialResponse credentialResponse = credentialIssuerService.issueCredentials(credentialRequest, accessToken);
            List<Credential> credentials = credentialResponse.getCredentials();
            assertAll(
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

    @DisplayName("When starting to issuing credentials in the pre-authorized code flow")
    @Nested
    class PreAuthorizedStartIssueTests {

        @DisplayName("then credential configuration must support the pre-authorized code flow")
        @Test
        void testConfigurationMustSupportPreAuthorizedCodeFlow() {
            when(credentialIssuerServerProperties.findCredentialConfiguration(eq("foo"))).thenReturn(CredentialConfigurationProperties.builder()
                    .identifier("foo")
                    .grantType("authorization_code")
                    .build());
            StartIssuanceRequest startIssuanceRequest = StartIssuanceRequest.builder().credentialConfigurationId("foo").build();
            IssuerServerException e = assertThrows(IssuerServerException.class, () -> credentialIssuerService.startIssuerTransaction(startIssuanceRequest, mock(JWT.class)));
            assertAll(
                    () -> assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus()),
                    () -> assertEquals("invalid_request", e.getError()),
                    () -> assertEquals("Credential configuration can only be used with the pre-authorized code flow", e.getErrorDescription())
            );
            verifyNoInteractions(auditService);
        }

    }

    @DisplayName("When creating credential offers for the authorization code flow")
    @Nested
    class CredentialOfferTests {

        @DisplayName("then a credential offer is generated for a credential configuration supporting the authorization code flow")
        @Test
        void testCreateCredentialOfferForAuthorizationCodeFlow() {
            when(credentialIssuerServerProperties.getCredentialIssuer()).thenReturn(URI.create("https://junit.issuer.idporten.no"));
            when(credentialIssuerServerProperties.findCredentialConfiguration(eq("foo"))).thenReturn(CredentialConfigurationProperties.builder()
                    .identifier("foo")
                    .grantType("authorization_code")
                    .build());
            CredentialOffer credentialOffer = credentialIssuerService.createCredentialOffer("foo");
            assertAll(
                    () -> assertEquals("https://junit.issuer.idporten.no", credentialOffer.getCredentialIssuer()),
                    () -> assertEquals(1, credentialOffer.getCredentialConfigurationIds().size()),
                    () -> assertEquals("foo", credentialOffer.getCredentialConfigurationIds().getFirst()),
                    () -> assertNotNull(credentialOffer.getGrants().getAuthorizedCodeGrant()),
                    () -> assertNull(credentialOffer.getGrants().getPreAuthorizedCodeGrant())
            );
            verify(auditService).logCreateCredentialOffer(eq("https://junit.issuer.idporten.no"), eq("foo"));
        }

        @DisplayName("then an error is returned for credential configurations supporting only the pre-authorized code flow")
        @Test
        void testDenyCredentialOfferForPreAuthorizedCodeFlow() {
            when(credentialIssuerServerProperties.findCredentialConfiguration(eq("foo"))).thenReturn(CredentialConfigurationProperties.builder()
                    .identifier("foo")
                    .grantType("urn:ietf:params:oauth:grant-type:pre-authorized_code")
                    .build());
            IssuerServerException e = assertThrows(IssuerServerException.class, () -> credentialIssuerService.createCredentialOffer("foo"));
            assertAll(
                    () -> assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus()),
                    () -> assertEquals("invalid_request", e.getError()),
                    () -> assertEquals("Credential configuration cannot be used with the authorization code flow", e.getErrorDescription())
            );
            verifyNoInteractions(auditService);
        }

    }


}
