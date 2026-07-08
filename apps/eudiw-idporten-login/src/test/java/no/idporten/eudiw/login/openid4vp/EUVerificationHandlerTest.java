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

        @DisplayName("then the query contains exactly five claims")
        @Test
        void testContainsFiveClaims() {
            assertEquals(5, dcqlQuery.credentials().getFirst().claims().size());
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
            assertEquals(VerificationHandler.AMR_EUDIW, authorization.getAmr());
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
            assertEquals("996-09-25", authorization.getAttributes().get("birthdate"));
        }
    }
}
