package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jwt.JWTClaimsSet;
import no.idporten.eudiw.issuer.credentials.status.StatusList;
import no.idporten.eudiw.issuer.jwt.InvalidClaimException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("When reading attestation status from claims")
class AttestationStatusTest {

    @DisplayName("then status list reference and exp are read")
    @Test
    void testReadAttestationStatus() throws Exception {
        JWTClaimsSet claims = JWTClaimsSet.parse("""
                {"client_status": {"status": {"status_list": {"idx": 1337, "uri": "https://revocation_url/wia-statuslists/42"}}, "exp": 1303497780}}
                """);

        AttestationStatus attestationStatus = AttestationStatus.fromClaims(claims, "client_status");

        assertAll(
                () -> assertEquals(new StatusList(1337, URI.create("https://revocation_url/wia-statuslists/42")), attestationStatus.statusList()),
                () -> assertEquals(new Date(1303497780000L), attestationStatus.exp())
        );
    }

    @DisplayName("then a missing claim gives null")
    @Test
    void testMissingClaim() throws Exception {
        JWTClaimsSet claims = JWTClaimsSet.parse("""
                {"key_storage_status": {"exp": 1303497780}}
                """);

        assertNull(AttestationStatus.fromClaims(claims, "client_status"));
    }

    @DisplayName("then missing status and exp give null fields")
    @Test
    void testMissingStatusAndExp() throws Exception {
        JWTClaimsSet claims = JWTClaimsSet.parse("""
                {"client_status": {}}
                """);

        AttestationStatus attestationStatus = AttestationStatus.fromClaims(claims, "client_status");

        assertAll(
                () -> assertNull(attestationStatus.statusList()),
                () -> assertNull(attestationStatus.exp())
        );
    }

    @DisplayName("then a status list without idx is invalid")
    @Test
    void testStatusListWithoutIndex() throws Exception {
        JWTClaimsSet claims = JWTClaimsSet.parse("""
                {"client_status": {"status": {"status_list": {"uri": "https://revocation_url/wia-statuslists/42"}}}}
                """);

        InvalidClaimException e = assertThrows(InvalidClaimException.class, () -> AttestationStatus.fromClaims(claims, "client_status"));
        assertEquals("Claim client_status.status.status_list.idx is missing", e.getMessage());
    }

    @DisplayName("then a status list without uri is invalid")
    @Test
    void testStatusListWithoutUri() throws Exception {
        JWTClaimsSet claims = JWTClaimsSet.parse("""
                {"client_status": {"status": {"status_list": {"idx": 1337}}}}
                """);

        InvalidClaimException e = assertThrows(InvalidClaimException.class, () -> AttestationStatus.fromClaims(claims, "client_status"));
        assertEquals("Claim client_status.status.status_list.uri is missing", e.getMessage());
    }

    @DisplayName("then an exp that is not a number is invalid")
    @Test
    void testInvalidExp() throws Exception {
        JWTClaimsSet claims = JWTClaimsSet.parse("""
                {"client_status": {"exp": "tomorrow"}}
                """);

        assertThrows(InvalidClaimException.class, () -> AttestationStatus.fromClaims(claims, "client_status"));
    }

}
