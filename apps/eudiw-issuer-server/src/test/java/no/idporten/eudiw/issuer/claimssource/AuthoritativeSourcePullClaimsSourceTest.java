package no.idporten.eudiw.issuer.claimssource;


import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSourceService;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.issuance.CredentialIssuanceType;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.issuer.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;


@DisplayName("When issuing a credential with credential data from an authoritative source")
@ExtendWith(MockitoExtension.class)
public class AuthoritativeSourcePullClaimsSourceTest {

    @Mock
    AuthoritativeSourceService authoritativeSourceService;

    @InjectMocks
    AuthoritativeSourcePullClaimsSource claimsSource;

    @BeforeEach
    void setUp() {
        claimsSource.setClaimsSourceCache(new InMemoryClaimsSourceCache());
    }

    @DisplayName("then the credential data is retrieved from the authoritative source service in pre-authorized code flow")
    @Test
    void testPreAuthorizedCodeFlowPullFromAuthoritativeSource() {
        final ExtendedCredentialConfiguration credentialConfiguration = credentialConfigurationFromClasspath("credential-configurations/advokattilsynet/advokatbevilling_mso_mdoc.json");
        final IssuanceTransactionId transactionId = new IssuanceTransactionId();
        final String personIdentifier = syntheticPersonIdentifier();
        final JWT preAuthorizedAccessToken = preAuthAccessToken(personIdentifier, transactionId);
        final CredentialData testCredentialData = new CredentialData(
                Map.of(
                "etternavn", "FOT",
                "fornavn", "ØKONOMISK INITIATIVRIK",
                "personidentifikator", "16903349844",
                "regnr", "47756",
                "tittel", "Advokat"
         )
        );
        PreAuthorizedIssuanceContext issuanceContext = new PreAuthorizedIssuanceContext(junitIssuerTenant(), credentialConfiguration, transactionId, preAuthorizedAccessToken);
        when(authoritativeSourceService.retrieveCredentialData(eq("advokatregisteret"), eq("no.advokattilsynet.advokatregisteret.1"), eq(personIdentifier))).thenReturn(testCredentialData);
        claimsSource.preAuthorize(issuanceContext, null);
        JWT walletAccessToken = preAuthAccessToken(personIdentifier, transactionId);
        CredentialIssueContext issuerContext = new CredentialIssueContext(walletAccessToken, junitIssuerTenant(), credentialConfiguration, transactionId, CredentialIssuanceType.PRE_AUTHORIZED_CODE);
        List<Claim> claims = claimsSource.issueClaims(issuerContext);
        assertAll(
                () -> assertEquals("FOT", findClaimValue(claims, "etternavn")),
                () -> assertEquals("ØKONOMISK INITIATIVRIK", findClaimValue(claims, "fornavn")),
                () -> assertEquals("16903349844", findClaimValue(claims, "personidentifikator")),
                () -> assertEquals("47756", findClaimValue(claims, "regnr")),
                () -> assertEquals("Advokat", findClaimValue(claims, "tittel"))
        );
    }

    @DisplayName("then the credential data is retrieved from the authoritative source service in authorization code flow")
    @Test
    void testAuthorizationCodeFlowPullFromAuthoritativeSource() {
        final ExtendedCredentialConfiguration credentialConfiguration = credentialConfigurationFromClasspath("credential-configurations/ageverification/proof_of_age_mso_mdoc.json");
        final String personIdentifier = syntheticPersonIdentifier();
        final CredentialData testCredentialData = new CredentialData(
                Map.of(
                        "age_over_15", true,
                        "age_over_18", false
                )
        );
        when(authoritativeSourceService.retrieveCredentialData(eq("freg"), eq("eu.europa.ec.av.1"), eq(personIdentifier))).thenReturn(testCredentialData);
        JWT walletAccessToken = accessToken(personIdentifier);
        CredentialIssueContext issuerContext = new CredentialIssueContext(
                walletAccessToken,
                junitIssuerTenant(),
                credentialConfiguration,
                null,
                CredentialIssuanceType.AUTHORIZATION_CODE
        );
        List<Claim> claims = claimsSource.issueClaims(issuerContext);
        assertAll(
                () -> assertEquals(true, findClaimValue(claims, "age_over_15")),
                () -> assertEquals(false, findClaimValue(claims, "age_over_18"))
        );
    }

    private Object findClaimValue(List<Claim> claims, String claimName) {
        return claims.stream().filter(c -> c.getPath().getLast().equals(claimName)).findFirst().orElseThrow().getValue().value();
    }

}
