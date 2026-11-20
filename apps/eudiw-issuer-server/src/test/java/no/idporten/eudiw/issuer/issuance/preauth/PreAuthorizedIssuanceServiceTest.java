package no.idporten.eudiw.issuer.issuance.preauth;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.authoritativesources.JUnitPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.CredentialIssuerContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.issuance.preauth.integration.PreAuthorizationIntegration;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.metrics.MetricService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static no.idporten.eudiw.issuer.TestData.junitCredentialConfiguration;
import static no.idporten.eudiw.issuer.TestData.junitIssuerTenant;

@DisplayName("When starting to issuing credentials in the pre-authorized code flow")
@ExtendWith(MockitoExtension.class)
public class PreAuthorizedIssuanceServiceTest {

    @Mock
    private ClaimsSourceService claimsSourceService;
    @Mock
    private AccessTokenValidationService accessTokenValidationService;
    @Mock
    private PreAuthorizationIntegration preAuthorizationIntegration;
    @Mock
    private CredentialIssuanceStatusService credentialIssuanceStatusService;
    @Mock
    private AuditService auditService;
    @Mock
    private MetricService metricService;
    @Mock
    private FeatureSwitches featureSwitches;

    @InjectMocks
    private PreAuthorizedIssuanceService preAuthorizedIssuanceService;

    @DisplayName("then tx_code is included only when both server and credential enable it")
    @ParameterizedTest
    @CsvSource({
            "false, true, false",
            "true, false, false",
            "true, true, true"
    })
    void testTxCodeRequiresBothServerAndCredentialConfiguration(boolean serverEnabled, boolean credentialRequiresTxCode, boolean expectedTxCode) throws Exception {
        when(featureSwitches.isPreAuthTxCodeEnabled()).thenReturn(serverEnabled);
        when(preAuthorizationIntegration.preAuthorize(any(), any(), any())).thenReturn("pac");
        when(claimsSourceService.findClaimsSource(any())).thenReturn(mock(JUnitPreAuthorizedClaimsSource.class));
        CredentialIssuerTenant credentialIssuerTenant = spy(junitIssuerTenant());
        ExtendedCredentialConfiguration credentialConfiguration = junitCredentialConfiguration();
        CredentialIssuerContext credentialIssuerContext = credentialConfiguration.getCredentialIssuerContext();
        credentialIssuerContext.setGrantType(PreAuthorizedIssuanceService.GRANT_TYPE_PRE_AUTHORIZED_CODE);
        credentialIssuerContext.setRequireTxCode(credentialRequiresTxCode);
        doReturn(credentialConfiguration).when(credentialIssuerTenant).findCredentialConfiguration("junit-ccid");
        PreAuthorizedIssuanceRequest request = PreAuthorizedIssuanceRequest.builder()
                .credentialIssuer(credentialIssuerTenant.getCredentialIssuer().toString())
                .credentialConfigurationId("junit-ccid")
                .build();

        String response = new ObjectMapper().writeValueAsString(
                preAuthorizedIssuanceService.startIssuerTransaction(credentialIssuerTenant, request, mock(JWT.class)));
        Object txCode = JsonPath.parse(response, Configuration.defaultConfiguration()
                        .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL))
                .read("$.credential_offer.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code'].tx_code");
        assertEquals(expectedTxCode, txCode != null);
    }

    @DisplayName("then credential configuration must support the pre-authorized code flow")
    @Test
    void testConfigurationMustSupportPreAuthorizedCodeFlow() {
        CredentialIssuerTenant credentialIssuerTenant = spy(junitIssuerTenant());
        doReturn(ExtendedCredentialConfiguration.builder()
                .credentialConfigurationId("foo")
                .credentialIssuerContext(CredentialIssuerContext.builder().grantType("authorization_code").build())
                .build())
                .when(credentialIssuerTenant).findCredentialConfiguration(eq("foo"));
        PreAuthorizedIssuanceRequest preAuthorizedIssuanceRequest = PreAuthorizedIssuanceRequest.builder().credentialIssuer(credentialIssuerTenant.getCredentialIssuer().toString()).credentialConfigurationId("foo").build();
        IssuerServerException e = assertThrows(IssuerServerException.class, () -> preAuthorizedIssuanceService.startIssuerTransaction(credentialIssuerTenant, preAuthorizedIssuanceRequest, mock(JWT.class)));
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus()),
                () -> assertEquals("invalid_request", e.getError()),
                () -> assertEquals("Credential configuration can only be used with the pre-authorized code flow", e.getErrorDescription())
        );
        verifyNoInteractions(auditService);
    }
}
