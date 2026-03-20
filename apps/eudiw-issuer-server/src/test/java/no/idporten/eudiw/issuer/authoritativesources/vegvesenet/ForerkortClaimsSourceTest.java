package no.idporten.eudiw.issuer.authoritativesources.vegvesenet;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.config.ClasspathSingleCredentialConfigurationSource;
import no.idporten.eudiw.issuer.config.CredentialConfigurationSource;
import no.idporten.eudiw.issuer.config.CredentialConfigurationSourceProperties;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static no.idporten.eudiw.issuer.TestData.junitCredentialConfiguration;
import static no.idporten.eudiw.issuer.TestData.junitIssuerTenant;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ForerkortClaimsSourceTest {

    @Test
    @DisplayName("when call push with 4 claims, then all required claims from metadata are included in push result")
    void allRequiredClaimsFromMetadataAreIncludedInPushResult() {
        ForerkortClaimsSource source = new ForerkortClaimsSource();
        Map<String, String> inputClaims = new HashMap<>();
        inputClaims.put("family_name", "Testesen");
        inputClaims.put("given_name", "Test");
        inputClaims.put("birth_date", "1990-01-01");
        inputClaims.put("portrait", "0");
        inputClaims.put("age_over_16", "true");
        inputClaims.put("age_over_18", "true");
        inputClaims.put("age_over_21", "true");
        inputClaims.put("age_in_years", "25");

        JWT jwt = mock(JWT.class);
        IssuanceTransactionId txId = new IssuanceTransactionId("tx-123");

        CredentialData credentialData = source.push(new PreAuthorizedIssuanceContext(junitIssuerTenant(), junitCredentialConfiguration(), txId, jwt), new CredentialData(Collections.unmodifiableMap(inputClaims)));
        Map<String, Object> result = credentialData.claims();
        CredentialConfigurationSource credentialConfigurationSource = new ClasspathSingleCredentialConfigurationSource(new CredentialConfigurationSourceProperties("classpath:credential-configurations/vegvesenet/mdl_mso_mdoc.json", null,null, null, null));
        credentialConfigurationSource.init();
        ExtendedCredentialConfiguration credentialConfiguration = credentialConfigurationSource.retrieve().getFirst();
        ExtendedCredentialMetadata credentialMetadata =  credentialConfiguration.getExtendedCredentialMetadata();
        Set<String> expectedClaims = credentialMetadata.claims().stream()
                .map(ExtendedClaimsDescription::name)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(result.keySet().containsAll(expectedClaims), "All claims should be present in result");
    }
}