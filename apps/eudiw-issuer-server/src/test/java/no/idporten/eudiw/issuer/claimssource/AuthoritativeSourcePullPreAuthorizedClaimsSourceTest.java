package no.idporten.eudiw.issuer.claimssource;


import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSourceService;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static no.idporten.eudiw.issuer.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;


@DisplayName("When issuing a credential with data from an authoritative source")
@ExtendWith(MockitoExtension.class)
public class AuthoritativeSourcePullPreAuthorizedClaimsSourceTest {

    @Mock
    AuthoritativeSourceService authoritativeSourceService;

    @InjectMocks
    AuthoritativeSourcePullPreAuthorizedClaimsSource claimsSource;

    @DisplayName("then the credential data is retrieved from the authoritative source service")
    @Test
    void testAdvokatregisteretAdvokatbevillingFromAuthoritativeSource() {
        final ExtendedCredentialConfiguration credentialConfiguration = credentialConfigurationFromClasspath("credential-configurations/advokattilsynet/advokatbevilling_mso_mdoc.json");
        final IssuanceTransactionId transactionId = new IssuanceTransactionId();
        final String personIdentifier = syntheticPersonIdentifier();
        final JWT accessToken = accessToken(personIdentifier, transactionId);
        final CredentialData testCredentialData = new CredentialData(
                Map.of(
                "etternavn", "FOT",
                "fornavn", "ØKONOMISK INITIATIVRIK",
                "personidentifikator", "16903349844",
                "regnr", "47756",
                "tittel", "Advokat"
         )
        );
        PreAuthorizedIssuanceContext issuanceContext = new PreAuthorizedIssuanceContext(junitIssuerTenant(), credentialConfiguration, transactionId, accessToken);
        when(authoritativeSourceService.retrieveCredentialData(eq("advokatregisteret"), eq("no.advokattilsynet.advokatregisteret.1"), eq(personIdentifier))).thenReturn(testCredentialData);
        CredentialData credentialData = claimsSource.pull(issuanceContext);
        assertEquals(testCredentialData, credentialData);
    }

}
