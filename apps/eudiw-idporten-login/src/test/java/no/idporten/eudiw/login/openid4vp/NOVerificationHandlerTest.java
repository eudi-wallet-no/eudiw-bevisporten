package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.TestData;
import no.idporten.eudiw.login.openid4vp.verifier.model.DcqlQuery;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling Norwegian users")
class NOVerificationHandlerTest {

    private NOVerificationHandler handler = new NOVerificationHandler();

    @DisplayName("When creating a DCQL query for Norwegian PID")
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

        @DisplayName("then the query contains exactly three claims")
        @Test
        void testContainsThreeClaims() {
            assertEquals(3, dcqlQuery.credentials().getFirst().claims().size());
        }

        @DisplayName("then the claims include family_name")
        @Test
        void testClaimsIncludeFamilyName() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(java.util.List.of("eu.europa.ec.eudi.pid.1", "family_name"))));
        }

        @DisplayName("then the claims include given_name")
        @Test
        void testClaimsIncludeGivenName() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(java.util.List.of("eu.europa.ec.eudi.pid.1", "given_name"))));
        }

        @DisplayName("then the claims include personal_administrative_number")
        @Test
        void testClaimsIncludePersonalAdministrativeNumber() {
            assertTrue(dcqlQuery.credentials().getFirst().claims().stream()
                    .anyMatch(c -> c.path().equals(java.util.List.of("eu.europa.ec.eudi.pid.1", "personal_administrative_number"))));
        }
    }

    @DisplayName("When completing verification with a Norwegian PID credential")
    @Nested
    class CompleteVerificationTests {

        private Authorization authorization;

        @BeforeEach
        void completeVerification() {
            authorization = handler.completeVerification(TestData.verifiedCredentialNO());
        }

        @DisplayName("then sub is set to the personal administrative number")
        @Test
        void testSubIsPersonalAdministrativeNumber() {
            assertEquals(TestData.syntheticPersonIdentifier(), authorization.getSub());
        }

        @DisplayName("then acr is idporten-loa-high")
        @Test
        void testAcrIsIdportenLoaHigh() {
            assertEquals(AcrValue.IDPORTEN_LOA_HIGH.value(), authorization.getAcr());
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
            assertEquals("LEGITIM", authorization.getAttributes().get("given_name"));
        }
    }
}
