package no.idporten.eudiw.issuer.claimssource.advokattilsynet;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.lib.maskinporten.client.MaskinportenClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@DisplayName("When issuing documents for Advokatregisteret")
@ActiveProfiles("junit")
@SpringBootTest
public class AdvokatregisteretClaimsSourceTest {

    @Autowired
    AdvokatregisteretClaimsSource claimsSource;

    @Qualifier("advokatregisteretMaskinportenClient")
    @Autowired
    MaskinportenClient maskinportenClient;

    @MockitoBean
    AdvokatregisteretIntegration advokatregisteretIntegration;

    @DisplayName("then the claims source is loaded and initialized")
    @Test
    void testClaimsSourceInitialized() {
        assertAll(
                () -> assertNotNull(claimsSource),
                () -> assertNotNull(claimsSource.getProperties()),
                () -> assertTrue(claimsSource.supports("no.advokattilsynet.advokatregisteret.1"))
        );
    }

    @DisplayName("then claims source metadata is provided to the issuer")
    @Test
    void testMetadata() {
        ClaimsSourceMetadata claimsSourceMetadata = claimsSource.getMetadata();
        assertAll(
                () -> assertEquals(6, claimsSourceMetadata.getClaims().size()),
                () -> assertEquals(
                        "Personidentifikator",
                        claimsSourceMetadata
                                .findClaimsDescription("personidentifikator")
                                .findDisplay("no")
                                .getName()),
                () -> assertEquals(
                        "Tittel",
                        claimsSourceMetadata
                                .findClaimsDescription("tittel")
                                .findDisplay("no")
                                .getName()),
                () -> assertEquals(
                        "Etternavn",
                        claimsSourceMetadata
                                .findClaimsDescription("etternavn")
                                .findDisplay("no")
                                .getName()),
                () -> assertEquals(
                        "Fornavn",
                        claimsSourceMetadata
                                .findClaimsDescription("fornavn")
                                .findDisplay("no")
                                .getName()),
                () -> assertFalse(claimsSourceMetadata.findClaimsDescription("mellomnavn").isMandatory()),
                () -> assertTrue(claimsSourceMetadata.findClaimsDescription("regnr").isMandatory())
        );
    }

    @DisplayName("then push is not supported")
    @Test
    void testPushNotSupported() {
        assertThrows(IssuerServerException.class, () -> claimsSource.push(new IssuanceTransactionId(), null, Collections.emptyMap()));
    }

    @DisplayName("then data can be pulled from authoritative source")
    @Test
    void testPullFromAuthoritativeSource() throws Exception {
        String personIdentifier = "12345678901";
        JWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().claim("pid", personIdentifier).build());
        String response = """
                {
                    "regnr": "47756",
                    "tittel": "Advokat",
                    "fornavn": "ØKONOMISK INITIATIVRIK",
                    "mellomnavn": null,
                    "etternavn": "FOT",
                    "tilknyttedePraksiser": [
                        {
                            "organisasjonsNummer": 314259173,
                            "hovedpraksis": false
                        }
                    ]
                }""";
        PersonPrivate personPrivate = new ObjectMapper().readValue(response, PersonPrivate.class);
        when(advokatregisteretIntegration.retrieve(eq(personIdentifier))).thenReturn(personPrivate);
        Map<String, String> claims = claimsSource.pull(new IssuanceTransactionId(), accessToken);
        assertAll(
                () -> assertEquals(5, claims.size()),
                () -> assertEquals(personIdentifier, claims.get("personidentifikator")),
                () -> assertEquals("47756", claims.get("regnr")),
                () -> assertEquals("Advokat", claims.get("tittel")),
                () -> assertEquals("ØKONOMISK INITIATIVRIK", claims.get("fornavn")),
                () -> assertEquals("FOT", claims.get("etternavn")),
                () -> assertFalse(claims.containsKey("mellomnavn"))
        );
    }

}
