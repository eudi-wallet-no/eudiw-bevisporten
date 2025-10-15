package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.StringValue;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@DisplayName("NnidClaimsSource tests")
class NnidClaimsSourceTest {

    public static final String NORWEGIAN_NATIONAL_ID_NUMBER = "norwegian_national_id_number";
    public static final String NORWEGIAN_NATIONAL_ID_NUMBER_TYPE = "norwegian_national_id_number_type";
    public static final String NORWEGIAN_NATIONAL_ID_NUMBER_DOCTYPE = "no.skatteetaten.nnid.1";

    private NnidClaimsSource nnidClaimsSource;

    @BeforeEach
    void setUp() {
        nnidClaimsSource = new NnidClaimsSource();
        ClaimsSourceProperties properties = new ClaimsSourceProperties();
        properties.setCredentialType(NORWEGIAN_NATIONAL_ID_NUMBER_DOCTYPE);
        nnidClaimsSource.init(properties);
    }

    @DisplayName("validation of claims")
    @Nested
    class ValidationTests {

        @DisplayName("for claim norwegian_national_id_number is valid")
        @Test
        void validateClaimFnrIs11Digits() {
            ClaimMetadata claimMetadata = nnidClaimsSource.getDocumentMetadata().findClaimMetadata(NORWEGIAN_NATIONAL_ID_NUMBER);

            nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER, "12345678901"));
        }

        @DisplayName("for claim norwegian_national_id_number is invalid when not 11 digits")
        @Test
        void validateClaimTextFnrIsNot11Digits() {
            ClaimMetadata claimMetadata = nnidClaimsSource.getDocumentMetadata().findClaimMetadata(NORWEGIAN_NATIONAL_ID_NUMBER);
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER, "abcdefghijk")));
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER, "4d")));
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER, "")));
        }

        @DisplayName("for claim norwegian_national_id_number_type is valid when D-nummer or F-nummer")
        @Test
        void validateClaimTypeIsValidTerm() {
            ClaimMetadata claimMetadata = nnidClaimsSource.getDocumentMetadata().findClaimMetadata(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE);
            nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer"));
            nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "F-nummer"));
        }

        @DisplayName("for claim norwegian_national_id_number_type is invalid when random text or empty string")
        @Test
        void validateClaimTypeIsNotValidTerm() {
            ClaimMetadata claimMetadata = nnidClaimsSource.getDocumentMetadata().findClaimMetadata(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE);
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(claimMetadata, Map.of(NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "")));
        }

        @DisplayName("when all required claims are present and valid then all is valid")
        @Test
        void validateAllClaimsOK() {
            nnidClaimsSource.validate(
                    Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER, "11127911122",
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "Fødselsnummer"));
        }

        @DisplayName("when all required claims are present, but 1 invalid then all is invalid")
        @Test
        void validateOneClaimNotOK() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validate
                    (Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER, "4444",
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer")));
        }

        @DisplayName("when not all required claims are present, then all is invalid")
        @Test
        void validateMissingClaimNotOK() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validate(
                    Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer")));
        }

        @DisplayName("when all required claims are present and valid but also extra claim present, then all is invalid")
        @Test
        void validateExtraClaimNotOK() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validate(
                    Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER, "12345678901",
                            "extra-claim", "try-to-stop-me",
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer")));
        }
    }

    @DisplayName("When using the claims source lifecycle methods")
    @Nested
    class LifecycleTest {

        @DisplayName("then pre-authorized data are validated, stored, retrieved and formatted")
        @Test
        void testClaimsSourceLifecycle() {
            final String transactionId = "tid";
            JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                    .issuer("https://junit.idporten.no")
                    .claim("scope", "openid profile foo:bar")
                    .claim("tx_id", transactionId)
                    .build();
            PlainJWT accessToken = new PlainJWT(jwtClaimsSet);

            NnidClaimsSource claimsSource = spy(nnidClaimsSource);
            claimsSource.preAuthorize(
                    transactionId,
                    accessToken,
                    new TreeMap<>(Map.of(
                            NORWEGIAN_NATIONAL_ID_NUMBER, "12345678901",
                            NORWEGIAN_NATIONAL_ID_NUMBER_TYPE, "D-nummer")));
            verify(claimsSource).validate(anyMap());
            List<Claim> claims = nnidClaimsSource.retrieveClaims(accessToken);
            assertAll(
                    () -> assertEquals(2, claims.size()),
                    () -> assertEquals("12345678901", ((StringValue)claims.getFirst().getValue()).value()),
                    () -> assertEquals(List.of("no.skatteetaten.nnid.1", "norwegian_national_id_number"), claims.getFirst().getPath()),
                    () -> assertEquals("D-nummer", ((StringValue)claims.getLast().getValue()).value()),
                    () -> assertEquals(List.of("no.skatteetaten.nnid.1", "norwegian_national_id_number_type"), claims.getLast().getPath())
            );
        }
    }

}