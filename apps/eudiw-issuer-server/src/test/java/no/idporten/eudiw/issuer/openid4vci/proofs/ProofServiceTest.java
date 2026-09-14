package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.openid4vci.protocol.Proofs;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ProofServiceTest {

    @Spy
    CredentialIssuerServerProperties credentialIssuerServerProperties;

    @InjectMocks
    ProofService proofService;


    Proofs createJWTProofs(String... jwts) {
        Proofs proof = new Proofs();
        proof.setJwt(List.of(jwts));
        return proof;
    }

    Proofs createAttestationProofs(String... attestations) {
        Proofs proof = new Proofs();
        proof.setAttestation(List.of(attestations));
        return proof;
    }

    @BeforeEach
    void setUpProperties() {
        credentialIssuerServerProperties.setProofSigningAlgorithms(List.of(JWSAlgorithm.ES256.getName()));
        credentialIssuerServerProperties.setCredentialIssuer(URI.create("https://utsteder.test.eidas2sandkasse.net"));
    }


    @Test
    void testValidProofFromAndroidWallet() throws Exception {
       String jwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6Im9wZW5pZDR2Y2ktcHJvb2Yrand0IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiOWRvODhIMGdTQWhfd1YxQzRFbF90dHlBejBQSVlXR2dOclVjbFVSMUNuNCIsInkiOiJwRUNocDhVaWVZZm5mSXQ4cWlQUmNLbVRVRDZGdTBkaXgySFVXX2xBVXk4In19.eyJhdWQiOiJodHRwczovL3V0c3RlZGVyLnRlc3QuZWlkYXMyc2FuZGthc3NlLm5ldCIsImlhdCI6MTc2Mjk1MTI4NSwiaXNzIjoid2FsbGV0LWRldiIsIm5vbmNlIjoiZzVsSERxVVdId1hlOGNpQlF2ZU8yMjQ4YzVQS3JSZDZranRfZ3NvR1dvSSJ9.5RL_YVDljEoWCZxyxWA2y5t0yBpSyOl91Om8huBuyPEOtfrAqULD2rvy79K7XCU65ZeuB2ySLynd03u0xAJq8Q";
       Proofs proofs = createJWTProofs(jwt);
        CredentialIssuerTenant credentialIssuerTenant = CredentialIssuerTenant.builder()
                .credentialIssuer(URI.create("https://utsteder.test.eidas2sandkasse.net"))
                .build();
       List<JWK> bindingKeys = proofService.validateProofs(credentialIssuerTenant, proofs);
       assertEquals(1, bindingKeys.size());
    }

    @Test
    void shouldReturnAllKeysFromValidKeyAttestation() throws Exception {
        ECKey attestationSigningKey = generateKey();
        ECKey firstAttestedKey = generateKey();
        ECKey secondAttestedKey = generateKey();
        String keyAttestation = createKeyAttestation(
                attestationSigningKey,
                createCertificate(attestationSigningKey),
                List.of(firstAttestedKey, secondAttestedKey),
                "nnn",
                true);
        String jwtProof = createJwtProof(firstAttestedKey, keyAttestation);

        List<JWK> bindingKeys = proofService.validateProofs(issuerTenant(), createJWTProofs(jwtProof));

        assertEquals(
                List.of(firstAttestedKey.toPublicJWK(), secondAttestedKey.toPublicJWK()),
                bindingKeys);
    }

    @Test
    void shouldReturnAllKeysFromAttestationProof() throws Exception {
        ECKey attestationSigningKey = generateKey();
        ECKey firstAttestedKey = generateKey();
        ECKey secondAttestedKey = generateKey();
        String keyAttestation = createKeyAttestation(
                attestationSigningKey,
                createCertificate(attestationSigningKey),
                List.of(firstAttestedKey, secondAttestedKey),
                "nnn",
                true);

        List<JWK> bindingKeys = proofService.validateProofs(
                issuerTenant(),
                createAttestationProofs(keyAttestation));

        assertEquals(
                List.of(firstAttestedKey.toPublicJWK(), secondAttestedKey.toPublicJWK()),
                bindingKeys);
    }

    @Test
    void shouldRejectKeyAttestationWithInvalidSignature() throws Exception {
        ECKey certificateKey = generateKey();
        ECKey signingKey = generateKey();
        ECKey attestedKey = generateKey();
        String keyAttestation = createKeyAttestation(
                signingKey,
                createCertificate(certificateKey),
                List.of(attestedKey),
                "nnn",
                true);
        String jwtProof = createJwtProof(attestedKey, keyAttestation);

        assertThrows(
                InvalidProof.class,
                () -> proofService.validateProofs(issuerTenant(), createJWTProofs(jwtProof)));
    }

    @Test
    void shouldRejectKeyAttestationWithoutCertificate() throws Exception {
        ECKey attestationSigningKey = generateKey();
        ECKey attestedKey = generateKey();
        String keyAttestation = createKeyAttestation(
                attestationSigningKey,
                null,
                List.of(attestedKey),
                "nnn",
                false);
        String jwtProof = createJwtProof(attestedKey, keyAttestation);

        assertThrows(
                InvalidProof.class,
                () -> proofService.validateProofs(issuerTenant(), createJWTProofs(jwtProof)));
    }

    @Test
    void shouldRejectKeyAttestationWithoutAttestedKeys() throws Exception {
        ECKey attestationSigningKey = generateKey();
        ECKey proofSigningKey = generateKey();
        String keyAttestation = createKeyAttestation(
                attestationSigningKey,
                createCertificate(attestationSigningKey),
                null,
                "nnn",
                true);
        String jwtProof = createJwtProof(proofSigningKey, keyAttestation);

        assertThrows(
                InvalidProof.class,
                () -> proofService.validateProofs(issuerTenant(), createJWTProofs(jwtProof)));
    }

    @Test
    void shouldRejectProofNotSignedByAnAttestedKey() throws Exception {
        ECKey attestationSigningKey = generateKey();
        ECKey attestedKey = generateKey();
        ECKey proofSigningKey = generateKey();
        String keyAttestation = createKeyAttestation(
                attestationSigningKey,
                createCertificate(attestationSigningKey),
                List.of(attestedKey),
                "nnn",
                true);
        String jwtProof = createJwtProof(proofSigningKey, keyAttestation);

        assertThrows(
                InvalidProof.class,
                () -> proofService.validateProofs(issuerTenant(), createJWTProofs(jwtProof)));
    }

    private String createKeyAttestation(
            ECKey signingKey,
            X509Certificate certificate,
            List<ECKey> attestedKeys,
            String nonce,
            boolean includeCertificate) throws Exception {
        JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new JOSEObjectType("key-attestation+jwt"));
        if (includeCertificate) {
            header.x509CertChain(List.of(Base64.encode(certificate.getEncoded())));
        }
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim(
                        "attested_keys",
                        attestedKeys == null
                                ? null
                                : attestedKeys.stream().map(JWK::toPublicJWK).map(JWK::toJSONObject).toList())
                .claim("nonce", nonce)
                .build();
        SignedJWT keyAttestation = new SignedJWT(header.build(), claims);
        keyAttestation.sign(new ECDSASigner(signingKey));
        return keyAttestation.serialize();
    }

    private String createJwtProof(ECKey signingKey, String keyAttestation) throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new JOSEObjectType("openid4vci-proof+jwt"))
                .keyID("0")
                .customParam("key_attestation", keyAttestation)
                .build();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience(issuerTenant().getCredentialIssuer().toString())
                .issueTime(new Date())
                .claim("nonce", "nonce")
                .build();
        SignedJWT jwtProof = new SignedJWT(header, claims);
        jwtProof.sign(new ECDSASigner(signingKey));
        return jwtProof.serialize();
    }

    private X509Certificate createCertificate(ECKey key) throws Exception {
        Instant now = Instant.now();
        X500Name subject = new X500Name("CN=Key Attestation Test");
        var certificateBuilder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.ONE,
                Date.from(now.minusSeconds(60)),
                Date.from(now.plusSeconds(300)),
                subject,
                key.toECPublicKey());
        var contentSigner = new JcaContentSignerBuilder("SHA256withECDSA").build(key.toECPrivateKey());
        return new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(contentSigner));
    }

    private ECKey generateKey() throws Exception {
        return new ECKeyGenerator(Curve.P_256).generate();
    }

    private CredentialIssuerTenant issuerTenant() {
        return CredentialIssuerTenant.builder()
                .credentialIssuer(URI.create("https://utsteder.test.eidas2sandkasse.net"))
                .build();
    }

}
