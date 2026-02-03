package no.idporten.eudiw.issuer.authoritativesources.skatteetaten;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.*;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.credentials.types.ClaimMetadata;
import no.idporten.eudiw.issuer.credentials.types.StringValue;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@DisplayName("NnidClaimsSource tests")
class NnidClaimsSourceTest {

    public static final String NORWEGIAN_NATIONAL_ID_NUMBER = "norwegian_national_id_number";
    public static final String NORWEGIAN_NATIONAL_ID_NUMBER_TYPE = "norwegian_national_id_number_type";
    public static final String NORWEGIAN_NATIONAL_ID_NUMBER_NAMESPACE = "no.skatteetaten.nnid.1";
    public static final String NORWEGIAN_NATIONAL_ID_NUMBER_DOCTYPE = "no.skatteetaten.nnid.1";

    private NnidClaimsSource nnidClaimsSource;

    @BeforeEach
    void setUp() {
        nnidClaimsSource = new NnidClaimsSource();
        nnidClaimsSource.setClaimsSourceCache(new InMemoryClaimsSourceCache());
        ClaimsSourceProperties properties = new ClaimsSourceProperties();
        properties.setCredentialTypes(Set.of(NORWEGIAN_NATIONAL_ID_NUMBER_DOCTYPE));
        nnidClaimsSource.init(properties);
    }

    @DisplayName("validation of claims")
    @Nested
    class ValidationTests {

        @DisplayName("for claim norwegian_national_id_number is valid")
        @Test
        void validateClaimFnrIs11Digits() {
            ClaimMetadata claimMetadata = nnidClaimsSource.getDocumentMetadata(null).findClaimMetadata(NORWEGIAN_NATIONAL_ID_NUMBER);

            nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER, "12345678901"));
        }

        @DisplayName("for claim norwegian_national_id_number is invalid when not 11 digits")
        @Test
        void validateClaimTextFnrIsNot11Digits() {
            ClaimMetadata claimMetadata = nnidClaimsSource.getDocumentMetadata(null).findClaimMetadata(NORWEGIAN_NATIONAL_ID_NUMBER);
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER, "abcdefghijk")));
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER, "4d")));
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER, "")));
        }

        @DisplayName("for claim norwegian_national_id_number_type is valid when D-nummer or F-nummer")
        @Test
        void validateClaimTypeIsValidTerm() {
            ClaimMetadata claimMetadata = nnidClaimsSource.getDocumentMetadata(null).findClaimMetadata(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE);
            nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer"));
            nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "F-nummer"));
        }

        @DisplayName("for claim norwegian_national_id_number_type is invalid when random text or empty string")
        @Test
        void validateClaimTypeIsNotValidTerm() {
            ClaimMetadata claimMetadata = nnidClaimsSource.getDocumentMetadata(null).findClaimMetadata(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE);
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "")));
        }

        @DisplayName("when all required claims are present and valid then all is valid")
        @Test
        void validateAllClaimsOK() {
            nnidClaimsSource.validate(
                    nnidClaimsSource.getDocumentMetadata(null),
                    new CredentialData(Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER, "11127911122",
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "Fødselsnummer"), null));
        }

        @DisplayName("when all required claims are present, but 1 invalid then all is invalid")
        @Test
        void validateOneClaimNotOK() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validate
                    (
                            nnidClaimsSource.getDocumentMetadata(null),
                            new CredentialData(Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER, "4444",
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer"), null)));
        }

        @DisplayName("when not all required claims are present, then all is invalid")
        @Test
        void validateMissingClaimNotOK() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validate(
                    nnidClaimsSource.getDocumentMetadata(null),
                    new CredentialData(Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer"), null)));
        }

        @DisplayName("when all required claims are present and valid but also extra claim present, then all is invalid")
        @Test
        void validateExtraClaimNotOK() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validate(
                    nnidClaimsSource.getDocumentMetadata(null),
                    new CredentialData(Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER, "12345678901",
                            "extra-claim", "try-to-stop-me",
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer"), null)));
        }
    }

    @DisplayName("When using the claims source lifecycle methods")
    @Nested
    class LifecycleTest {

        @DisplayName("then pre-authorized data are validated, stored, retrieved and formatted")
        @Test
        void testClaimsSourceLifecycle() {
            final IssuanceTransactionId transactionId = new IssuanceTransactionId();
            JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                    .issuer("https://junit.idporten.no")
                    .claim("scope", "openid profile foo:bar")
                    .claim("tx_id", transactionId.getValue())
                    .build();
            PlainJWT accessToken = new PlainJWT(jwtClaimsSet);

            NnidClaimsSource claimsSource = spy(nnidClaimsSource);
            claimsSource.preAuthorize(
                    new PreAuthorizedIssuanceContext(transactionId, "ccid", accessToken, Duration.ofMinutes(10)),
                    new CredentialData(new TreeMap<>(Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER, "12345678901",
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer")),
                            null));
            verify(claimsSource).validate(any(), any(CredentialData.class));
            List<Claim> claims = nnidClaimsSource.issueClaims(new CredentialIssueContext(accessToken, null));
            assertAll(
                    () -> assertEquals(2, claims.size()),
                    () -> assertEquals("12345678901", ((StringValue) claims.getFirst().getValue()).value()),
                    () -> assertEquals(List.of(NORWEGIAN_NATIONAL_ID_NUMBER_NAMESPACE, "norwegian_national_id_number"), claims.getFirst().getPath()),
                    () -> assertEquals("D-nummer", ((StringValue) claims.getLast().getValue()).value()),
                    () -> assertEquals(List.of(NORWEGIAN_NATIONAL_ID_NUMBER_NAMESPACE, "norwegian_national_id_number_type"), claims.getLast().getPath())
            );
        }
    }

}