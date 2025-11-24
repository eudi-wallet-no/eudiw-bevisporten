package no.idporten.eudiw.issuer.openid4vci.sdjwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import id.walt.sdjwt.VerificationResult;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.time.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When creating credentials in SD-JWT VC format")
@ActiveProfiles("junit")
@SpringBootTest
public class SDJWTServiceTest {

    @Autowired
    private SDJWTService sdjwtService;

    @BeforeAll
    static void addBouncyCastle() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @MockitoBean
    private AuditLogger auditLogger;

    private CredentialConfigurationProperties credentialConfigurationProperties(String credentialType, String keyStoreName, int validityDays) {
        CredentialConfigurationProperties credentialConfigurationProperties = new CredentialConfigurationProperties();
        credentialConfigurationProperties.setCredentialType(credentialType);
        credentialConfigurationProperties.setFormat(CredentialFormat.SD_JWT_VC);
        credentialConfigurationProperties.setKeyStoreName(keyStoreName);
        credentialConfigurationProperties.setValidityDays(validityDays);
        return credentialConfigurationProperties;
    }

    @DisplayName("then claims are disclosed and data formats are handled")
    @Test
    void testIssueSDJwt() throws Exception {
        Instant now = Instant.now();
        Claim stringClaim1 = buildClaim("foo", "string1", new StringValue("foobar"));
        Claim numberClaim = buildClaim("foo", "number1", new NumberValue(42L));
        Claim booleanClaim = buildClaim("foo", "boolean1", new BooleanValue(true));
        Claim fullDateClaim = buildClaim("foo", "fulldate1", new FullDateValue(LocalDate.of(2025, 11, 5)));
        Claim dateTimeClaim = buildClaim("foo", "datetime1", new DateTimeValue(ZonedDateTime.parse("2025-11-10T10:30:00+01:00[Europe/Paris]")));
        Claim listNumberClaim = buildClaim("foo", "listnumbers", new ListValue(List.of(new NumberValue(1L), new NumberValue(2L), new NumberValue(3L))));
        Claim mapBooleanClaim = buildClaim("foo", "mapbooleans", new MapValue(Map.of("JA", new BooleanValue(true), "NEI", new BooleanValue(false))));
        List<Claim> claims = List.of(stringClaim1, numberClaim, booleanClaim, fullDateClaim, dateTimeClaim, listNumberClaim, mapBooleanClaim);
        CredentialConfigurationProperties credentialConfigurationProperties = credentialConfigurationProperties(
                "urn:foo",
                "eaa-provider",
                30);
        JWK deviceKey = generateDeviceKey();
        SDJwt sdJwt = sdjwtService.createSDJwt(
                deviceKey,
                credentialConfigurationProperties,
                claims);
        String encodedSDJwt = sdjwtService.encode(sdJwt);
        assertAll(
                () -> assertNotNull(sdJwt)
        );
        SDJwt unverifiedSDJwt = SDJwt.Companion.parse(encodedSDJwt);
        JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
        assertAll(
                () -> assertEquals("dc+sd-jwt", jwsHeader.getType().getType()),
                () -> assertEquals(1, jwsHeader.getX509CertChain().size())
        );
        X509Certificate cert = X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
        JWSVerifier jwsVerifier = new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(JWSAlgorithm.ES256, null, jwsVerifier);
        VerificationResult<SDJwt> verificationResult = unverifiedSDJwt.verify(cryptoProvider, null);
        assertAll(
                () -> assertTrue(verificationResult.getVerified()),
                () -> assertTrue(verificationResult.getSignatureVerified()),
                () -> assertTrue(verificationResult.getDisclosuresVerified())
        );
        SDJwt verifiedSDJwt = verificationResult.getSdJwt();
        SignedJWT signedJwt = SignedJWT.parse(verifiedSDJwt.getJwt());
        assertAll(
                () -> assertEquals("https://junit.eidas2sandkasse.dev/", signedJwt.getJWTClaimsSet().getIssuer()),
                () -> assertEquals("urn:foo", signedJwt.getJWTClaimsSet().getStringClaim("vct")),
                () -> assertEquals(
                        Clock.systemUTC().instant().getEpochSecond(),
                        signedJwt.getJWTClaimsSet().getIssueTime().toInstant().getEpochSecond(),
                        1000),
                () -> assertEquals(
                        Clock.systemUTC().instant().getEpochSecond() + (60 * 60 * 24 * 30),
                        signedJwt.getJWTClaimsSet().getExpirationTime().toInstant().getEpochSecond(),
                        1000),
                () -> assertNull(signedJwt.getJWTClaimsSet().getNotBeforeTime()),
                () -> assertEquals(claims.size(), verifiedSDJwt.getDisclosures().size())
        );
        for (Claim claim : claims) {
            assertNull(signedJwt.getJWTClaimsSet().getClaim(claim.getPath().getLast()));
        }
        JWTClaimsSet verifiedClaims = JWTClaimsSet.parse(unverifiedSDJwt.getFullPayload().toString());
        assertAll(
                () -> assertEquals("foobar", verifiedClaims.getStringClaim("string1")),
                () -> assertEquals(42, verifiedClaims.getLongClaim("number1")),
                () -> assertEquals(true,verifiedClaims.getBooleanClaim("boolean1")),
                () -> assertEquals("2025-11-05", verifiedClaims.getStringClaim("fulldate1")),
                () -> assertEquals("2025-11-10", verifiedClaims.getStringClaim("datetime1")),
                () -> assertEquals(3,  verifiedClaims.getListClaim("listnumbers").size()),
                () -> assertArrayEquals(new Long[]{1L, 2L, 3L}, verifiedClaims.getListClaim("listnumbers").toArray()),
                () -> assertTrue((Boolean) verifiedClaims.getJSONObjectClaim("mapbooleans").get("JA")),
                () -> assertFalse((Boolean) verifiedClaims.getJSONObjectClaim("mapbooleans").get("NEI"))
        );
        assertAll(
                () -> assertTrue(deviceKey.isPrivate()),
                () -> assertNotNull(verifiedClaims.getJSONObjectClaim("cnf")),
                () -> assertEquals(
                        deviceKey.toPublicJWK().toJSONObject(),
                        verifiedClaims.getJSONObjectClaim("cnf").get("jwk"))
        );
    }

    private static Claim buildClaim(String path, String key, ClaimValue value) {
        return Claim.builder().path(path).path(key).value(value).build();
    }

    private static ECKey generateDeviceKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256).generate();
    }

}
