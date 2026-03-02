package no.idporten.eudiw.issuer.credentials.formats;

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
import no.idporten.eudiw.issuer.config.CredentialIssuerContext;
import no.idporten.eudiw.issuer.config.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.types.*;
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

import static no.idporten.eudiw.issuer.credentials.formats.SDJWTService.BINARY_DATA_PREFIX;
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

    private ExtendedCredentialConfiguration credentialConfiguration(String credentialType, String credentialSigningKeystore, int validityDays) {
        ExtendedCredentialConfiguration credentialConfiguration = new ExtendedCredentialConfiguration();
        credentialConfiguration.setCredentialType(credentialType);
        credentialConfiguration.setFormat(CredentialFormat.SD_JWT_VC);
        CredentialIssuerContext credentialIssuerContext = new CredentialIssuerContext();
        credentialIssuerContext.setCredentialSigningKeystore(credentialSigningKeystore);
        credentialIssuerContext.setValidityDays(validityDays);
        credentialConfiguration.setCredentialIssuerContext(credentialIssuerContext);
        return credentialConfiguration;
    }

    @DisplayName("then claims are disclosed and data formats are handled")
    @Test
    void testIssueSDJwt() throws Exception {
        Claim stringClaim1 = buildClaim("foo", "string1", new StringValue("foobar"));
        Claim numberClaim = buildClaim("foo", "number1", new NumberValue(42L));
        Claim binaryClaim = buildClaim("foo", "image", new BinaryValue(base64EncodeSmileyJpeg(), "image/jpeg"));
        Claim booleanClaim = buildClaim("foo", "boolean1", new BooleanValue(true));
        Claim fullDateClaim = buildClaim("foo", "fulldate1", new FullDateValue(LocalDate.of(2025, 11, 5)));
        Claim dateTimeClaim = buildClaim("foo", "datetime1", new DateTimeValue(ZonedDateTime.parse("2025-11-10T10:30:00Z")));
        Claim listNumberClaim = buildClaim("foo", "listnumbers", new ListValue(List.of(new NumberValue(1L), new NumberValue(2L), new NumberValue(3L))));
        Claim mapBooleanClaim = buildClaim("foo", "mapbooleans", new MapValue(Map.of("JA", new BooleanValue(true), "NEI", new BooleanValue(false))));
        List<Claim> claims = List.of(stringClaim1, numberClaim, binaryClaim, booleanClaim, fullDateClaim, dateTimeClaim, listNumberClaim, mapBooleanClaim);
        ExtendedCredentialConfiguration credentialConfiguration = credentialConfiguration(
                "urn:foo",
                "eaa-provider",
                30);
        JWK deviceKey = generateDeviceKey();
        SDJwt sdJwt = sdjwtService.createSDJwt(
                deviceKey,
                credentialConfiguration,
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
                () -> assertEquals(BINARY_DATA_PREFIX.formatted("image/jpeg") + base64EncodeSmileyJpeg(), verifiedClaims.getStringClaim("image")),
                () -> assertEquals(true, verifiedClaims.getBooleanClaim("boolean1")),
                () -> assertEquals("2025-11-05", verifiedClaims.getStringClaim("fulldate1")),
                () -> assertEquals("2025-11-10T10:30:00Z", verifiedClaims.getStringClaim("datetime1")),
                () -> assertEquals(3, verifiedClaims.getListClaim("listnumbers").size()),
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

    private static String base64EncodeSmileyJpeg() {
        return "/9j/4AAQSkZJRgABAQAAAQABAAD/4gHYSUNDX1BST0ZJTEUAAQEAAAHIAAAAAAQwAABtbnRyUkdCIFhZWiAH4AABAAEAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlkZXNjAAAA8AAAACRyWFlaAAABFAAAABRnWFlaAAABKAAAABRiWFlaAAABPAAAABR3dHB0AAABUAAAABRyVFJDAAABZAAAAChnVFJDAAABZAAAAChiVFJDAAABZAAAAChjcHJ0AAABjAAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBHAEJYWVogAAAAAAAAb6IAADj1AAADkFhZWiAAAAAAAABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9YWVogAAAAAAAA9tYAAQAAAADTLXBhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABtbHVjAAAAAAAAAAEAAAAMZW5VUwAAACAAAAAcAEcAbwBvAGcAbABlACAASQBuAGMALgAgADIAMAAxADb/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAAUABQDAREAAhEBAxEB/8QAGAABAAMBAAAAAAAAAAAAAAAACQUHCgv/xAAjEAACAgICAgMAAwAAAAAAAAAFBgQHAwgBAgkKABQVFhcY/8QAFAEBAAAAAAAAAAAAAAAAAAAAAP/EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAMAwEAAhEDEQA/AGi9iTR3yxbZK+uTZ4wthLISiVXtBbC/UrXl0/54JsUtikCeAVtxLEwMyRhLyEDDBnxZiyfZO2SEPI9yyIPynsxiCYA4PNDo37A976h+N9SqN3eLKdK8peArbiJtH3qOTTrVf2EcMy9LLbieb+px1gB/zg/MH7H65eMBcZRXMLFZsZjI2HQ1TePF5ZnvSzW6RYly1ffdwLNP12k3jZlR2IvWmpnrlWUwFCf5GVzV5kwOUYJJjnuRYMsXtgwZiU/NMhxI46VD6fAxNbFbuW14BfYx202A2DTbHtnVvelXhsWf+LZ4nQsZQpvKxKV2FFimJK2pMTxQTEtnqsirJ46J4xpZshlkGYOVjDl84THmk9rjXfZPT571P0TRbQKk9jkAskW/aFqDMtdQ68Q2jD+axpqwEXmmcYbXBjD9iYRikzZkJCGASPEXp2sLoaKjAQVP4SfXAtjbLR8ZsFa+3e2+luaxrIbSSHVdbcGFaCwV5AFrIYTZR0KbkrcnEYbDI5gxDJseNOgF0kSoGIRLPhn8YIwdATYfWuhds6vK0tsjVSfclWmiAcsQTHUbwRFZCq+QwkwxOP26d8MuAQgS8PHOKbAkxZPaNllwcuTJBmzI2cDB1u9d3w/6qW5Au+qtQV+Y/ACGIqmSLPd7JuQCiko07giPKqyraTc2LmBgDTcMGUvNZMYVbFyYNhkAJ0cR+1MlA13wP//Z";
    }
}
