package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jwt.JWTClaimsSet;
import no.idporten.eudiw.issuer.jwt.InvalidClaimException;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static no.idporten.eudiw.issuer.TestData.keyAttestationJwt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyAttestationTest {

    @Test
    void shouldReadCertification() {
        KeyAttestation keyAttestation = new KeyAttestation(keyAttestationJwt(new JWTClaimsSet.Builder()
                .claim("certification", "https://junit-certification.example/wscd/1337")
                .build()));

        assertEquals(URI.create("https://junit-certification.example/wscd/1337"), keyAttestation.certification());
    }

    @Test
    void shouldReturnNullCertificationWhenMissing() {
        KeyAttestation keyAttestation = new KeyAttestation(keyAttestationJwt(new JWTClaimsSet.Builder().build()));

        assertNull(keyAttestation.certification());
    }

    @Test
    void shouldRejectInvalidCertification() {
        var jwt = keyAttestationJwt(new JWTClaimsSet.Builder().claim("certification", 42).build());

        assertThrows(InvalidClaimException.class, () -> new KeyAttestation(jwt));
    }

}
