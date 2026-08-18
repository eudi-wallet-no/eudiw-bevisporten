package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.TestData;
import no.idporten.eudiw.login.openid4vp.verifier.model.DcqlQuery;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling EU users")
class EUVerificationHandlerTest {

    private EUVerificationHandler handler = new EUVerificationHandler();

    @DisplayName("When creating a DCQL query for EU PID")
    @Nested
    class CreateDcqlQueryTests {

        private static final String WALLET_INTERACTION_ID = "test-wallet-interaction-id";
        private DcqlQuery dcqlQuery;

        @BeforeEach
        void createQuery() {
            dcqlQuery = handler.createDcqlQuery(WALLET_INTERACTION_ID);
        }

        @DisplayName("then the query contains exactly one credential")
        @Test
        void testContainsOneCredential() {
            assertEquals(1, dcqlQuery.credentials().size());
        }

        @DisplayName("then the credential id matches the wallet interaction id")
        @Test
        void testCredentialIdIsWalletInteractionId() {
            assertEquals(WALLET_INTERACTION_ID, dcqlQuery.credentials().getFirst().id());
        }

        @DisplayName("then the credential format is mso_mdoc")
        @Test
        void testCredentialFormatIsMsoMdoc() {
            assertEquals("mso_mdoc", dcqlQuery.credentials().getFirst().format());
        }

        @DisplayName("then the credential doctype is eu.europa.ec.eudi.pid.1")
        @Test
        void testCredentialDoctypeIsEudiPid() {
            assertEquals("eu.europa.ec.eudi.pid.1", dcqlQuery.credentials().getFirst().meta().doctypeValue());
        }

        @DisplayName("then the query contains exactly eight claims")
        @Test
        void testContainsEightClaims() {
            assertEquals(8, dcqlQuery.credentials().getFirst().claims().size());
        }

        @DisplayName("then the claims include family_name")
        @Test
        void testClaimsIncludeFamilyName() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(List.of("eu.europa.ec.eudi.pid.1", "family_name"))));
        }

        @DisplayName("then the claims include given_name")
        @Test
        void testClaimsIncludeGivenName() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(List.of("eu.europa.ec.eudi.pid.1", "given_name"))));
        }

        @DisplayName("then the claims include birth_date")
        @Test
        void testClaimsIncludeBirthDate() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(List.of("eu.europa.ec.eudi.pid.1", "birth_date"))));
        }

        @DisplayName("then the claims include place_of_birth")
        @Test
        void testClaimsIncludePlaceOfBirth() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(List.of("eu.europa.ec.eudi.pid.1", "place_of_birth"))));
        }

        @DisplayName("then the claims include nationality")
        @Test
        void testClaimsIncludeNationality() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(List.of("eu.europa.ec.eudi.pid.1", "nationality"))));
        }

        @DisplayName("then the claims include issuing_authority")
        @Test
        void testClaimsIncludeIssuingAuthority() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(List.of("eu.europa.ec.eudi.pid.1", "issuing_authority"))));
        }

        @DisplayName("then the claims include issuing_country")
        @Test
        void testClaimsIncludeIssuingCountry() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(List.of("eu.europa.ec.eudi.pid.1", "issuing_country"))));
        }

        @DisplayName("then the claims include personal_administrative_number")
        @Test
        void testClaimsIncludePersonalAdministrativeNumber() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(List.of("eu.europa.ec.eudi.pid.1", "personal_administrative_number"))));
        }
    }

    @DisplayName("When completing verification with an EU PID credential")
    @Nested
    class CompleteVerificationTests {

        private Authorization authorization;

        @BeforeEach
        void completeVerification() {
            authorization = handler.completeVerification(TestData.verifiedCredentialEU());
        }

        @DisplayName("then acr is eidas-loa-high")
        @Test
        void testAcrIsEidasLoaHigh() {
            assertEquals(AcrValue.EIDAS_LOA_HIGH.value(), authorization.getAcr());
        }

        @DisplayName("then amr is EUDIW")
        @Test
        void testAmrIsEudiw() {
            assertEquals(OpenID4VPVerificationHandler.AMR_EUDIW, authorization.getAmr());
        }

        @DisplayName("then family_name attribute is set from credential")
        @Test
        void testFamilyNameAttributeIsSet() {
            assertEquals("LOMMEBOK", authorization.getAttributes().get("family_name"));
        }

        @DisplayName("then given_name attribute is set from credential")
        @Test
        void testGivenNameAttributeIsSet() {
            assertEquals("UFUNKSJONELL", authorization.getAttributes().get("given_name"));
        }

        @DisplayName("then birthdate attribute is mapped from birth_date in credential")
        @Test
        void testBirthdateAttributeIsSet() {
            assertEquals("1996-09-25", authorization.getAttributes().get("birthdate"));
        }

        @DisplayName("then nationalities attribute is set from credential attribute nationality")
        @Test
        void testNationalityAttributeIsSet() {
            @SuppressWarnings("unchecked")
            List<String> nationalities = (List<String>) authorization.getAttributes().get("nationalities");
            assertTrue(nationalities.contains("FI"));
        }

        @DisplayName("then issuing_authority attribute is set from credential")
        @Test
        void testIssuingAuthorityAttributeIsSet() {
            assertEquals("Finnish Border Guard", authorization.getAttributes().get("issuing_authority"));
        }

        @DisplayName("then issuing_country attribute is set from credential")
        @Test
        void testIssuingCountryAttributeIsSet() {
            assertEquals("FI", authorization.getAttributes().get("issuing_country"));
        }

        @DisplayName("then personal_administrative_number attribute is set from credential")
        @Test
        void testPersonalAdministrativeNumberAttributeIsSet() {
            assertEquals("123456789", authorization.getAttributes().get("personal_administrative_number"));
        }
    }

    @DisplayName("When calculating sub value")
    @Nested
    class SubCalculationTests {

        @DisplayName("then same data with personal_administrative_number produces same sub value")
        @Test
        void testSameDataWithPersonalAdministrativeNumberProducesSameSub() {
            var credential1 = TestData.verifiedCredentialEUWithPersonalAdministrativeNumber("123456789");
            var credential2 = TestData.verifiedCredentialEUWithPersonalAdministrativeNumber("123456789");

            var auth1 = handler.completeVerification(credential1);
            var auth2 = handler.completeVerification(credential2);

            assertEquals(auth1.getSub(), auth2.getSub());
        }

        @DisplayName("then same data without personal_administrative_number produces same sub value")
        @Test
        void testSameDataWithoutPersonalAdministrativeNumberProducesSameSub() {
            var credential1 = TestData.verifiedCredentialEUWithoutPersonalAdministrativeNumber();
            var credential2 = TestData.verifiedCredentialEUWithoutPersonalAdministrativeNumber();

            var auth1 = handler.completeVerification(credential1);
            var auth2 = handler.completeVerification(credential2);

            assertEquals(auth1.getSub(), auth2.getSub());
        }

        @DisplayName("then data with personal_administrative_number produces different sub than without")
        @Test
        void testWithAndWithoutPersonalAdministrativeNumberProduceDifferentSub() {
            var credentialWithPersonalAdministrativeNumber = TestData.verifiedCredentialEUWithPersonalAdministrativeNumber("123456789");
            var credentialWithoutPersonalAdministrativeNumber = TestData.verifiedCredentialEUWithoutPersonalAdministrativeNumber();

            var authWithPersonalAdministrativeNumber = handler.completeVerification(credentialWithPersonalAdministrativeNumber);
            var authWithoutPersonalAdministrativeNumber = handler.completeVerification(credentialWithoutPersonalAdministrativeNumber);

            assertNotEquals(authWithPersonalAdministrativeNumber.getSub(), authWithoutPersonalAdministrativeNumber.getSub());
        }
    }
}
