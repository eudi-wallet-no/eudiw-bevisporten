package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import org.junit.jupiter.api.*;

import java.util.Map;

import static no.idporten.eudiw.issuer.claimssource.skatteetaten.NnidClaimsSource.ATTRIBUTE_NNID;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NnidClaimsSource tests")
class NnidClaimsSourceTest {

    private NnidClaimsSource nnidClaimsSource;

    @BeforeEach
    void setUp() {
        nnidClaimsSource = new NnidClaimsSource();
        ClaimsSourceProperties properties = new ClaimsSourceProperties();
        nnidClaimsSource.init(properties);
    }

    @DisplayName("validation of claims")
    @Nested
    class ValidationTests {

        @DisplayName("for claim norwegian_national_id_number is valid")
        @Test
        void validateClaimFnrIs11Digits() {
            nnidClaimsSource.validateClaim(ATTRIBUTE_NNID, Map.of(ATTRIBUTE_NNID, "12345678901"));
        }

        @DisplayName("for claim norwegian_national_id_number is invalid when not 11 digits")
        @Test
        void validateClaimTextFnrIsNot11Digits() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(ATTRIBUTE_NNID, Map.of(ATTRIBUTE_NNID, "abcdefghijk")));
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(ATTRIBUTE_NNID, Map.of(ATTRIBUTE_NNID, "4d")));
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(ATTRIBUTE_NNID, Map.of(ATTRIBUTE_NNID, "")));
        }

        @DisplayName("for claim norwegian_national_id_number_status is valid when Kontrollert or Ikke kontrollert")
        @Test
        void validateClaimStatusIsValidTerm() {
            nnidClaimsSource.validateClaim(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, Map.of(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, "Kontrollert"));
            nnidClaimsSource.validateClaim(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, Map.of(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, "unik"));
            nnidClaimsSource.validateClaim(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, Map.of(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, "Ikke kontrollert"));
        }

        @DisplayName("for claim norwegian_national_id_number_status is invalid when random text or empty string")
        @Test
        void validateClaimStatusIsNotValidTerm() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, Map.of(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, "dsf")));
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, Map.of(NnidClaimsSource.ATTRIBUTE_NNID_STATUS, "")));
        }


        @DisplayName("for claim norwegian_national_id_number_type is valid when D-nummer or F-nummer")
        @Test
        void validateClaimTypeIsValidTerm() {
            nnidClaimsSource.validateClaim(NnidClaimsSource.ATTRIBUTE_NNID_TYPE, Map.of(NnidClaimsSource.ATTRIBUTE_NNID_TYPE, "D-nummer"));
            nnidClaimsSource.validateClaim(NnidClaimsSource.ATTRIBUTE_NNID_TYPE, Map.of(NnidClaimsSource.ATTRIBUTE_NNID_TYPE, "F-nummer"));
        }

        @DisplayName("for claim norwegian_national_id_number_type is invalid when random text or empty string")
        @Test
        void validateClaimTypeIsNotValidTerm() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(NnidClaimsSource.ATTRIBUTE_NNID_TYPE, Map.of(NnidClaimsSource.ATTRIBUTE_NNID_TYPE, "nummer")));
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validateClaim(NnidClaimsSource.ATTRIBUTE_NNID_TYPE, Map.of(NnidClaimsSource.ATTRIBUTE_NNID_TYPE, "")));
        }

        @DisplayName("when all required claims are present and valid then all is valid")
        @Test
        void validateAllClaimsOK() {
            nnidClaimsSource.validate(Map.of(ATTRIBUTE_NNID, "11127911122",
                    NnidClaimsSource.ATTRIBUTE_NNID_STATUS, "Kontrollert",
                    NnidClaimsSource.ATTRIBUTE_NNID_TYPE, "D-nummer"));
        }

        @DisplayName("when all required claims are present, but 1 invalid then all is invalid")
        @Test
        void validateOneClaimNotOK() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validate(Map.of(ATTRIBUTE_NNID, "4444",
                    NnidClaimsSource.ATTRIBUTE_NNID_STATUS, "Kontrollert",
                    NnidClaimsSource.ATTRIBUTE_NNID_TYPE, "D-nummer")));
        }

        @DisplayName("when not all required claims are present, then all is invalid")
        @Test
        void validateMissingClaimNotOK() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validate(Map.of(ATTRIBUTE_NNID, "4444",
                    NnidClaimsSource.ATTRIBUTE_NNID_TYPE, "D-nummer")));
        }

        @DisplayName("when all required claims are present and valid but also extra claim present, then all is invalid")
        @Test
        void validateExtraClaimNotOK() {
            assertThrows(IssuerServerException.class, () -> nnidClaimsSource.validate(Map.of(ATTRIBUTE_NNID, "4444",
                    NnidClaimsSource.ATTRIBUTE_NNID_STATUS, "Kontrollert",
                    "extra-claim", "try-to-stop-me",
                    NnidClaimsSource.ATTRIBUTE_NNID_TYPE, "D-nummer")));
        }
    }
}