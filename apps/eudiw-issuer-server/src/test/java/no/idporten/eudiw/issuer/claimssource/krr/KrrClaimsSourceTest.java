package no.idporten.eudiw.issuer.claimssource.krr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.krr.model.PersonKrr;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("When issuing documents for Krr")
@ActiveProfiles("junit")
@SpringBootTest
public class KrrClaimsSourceTest {

    @Autowired
    KrrClaimsSource claimsSource;

    @MockitoBean
    KrrIntegration krrIntegration;

    @DisplayName("then the claims source is loaded and initialized")
    @Test
    void testClaimsSourceInitialized() {
        assertAll(
                () -> assertNotNull(claimsSource),
                () -> assertNotNull(claimsSource.getProperties()),
                () -> assertTrue(claimsSource.supports("no.kontaktregisteret.kontaktinformasjon.1"))
        );
    }

    @DisplayName("then claims source metadata is provided to the issuer")
    @Test
    void testMetadata() {
        DocumentMetadata claimsSourceMetadata = claimsSource.getDocumentMetadata();
        assertAll(
                () -> assertEquals(3, claimsSourceMetadata.claims().size()),
                () -> assertEquals(
                        "Personidentifikator",
                        claimsSourceMetadata
                                .findClaimMetadata("personidentifikator")
                                .getDisplayName("no")),
                () -> assertEquals(
                        "Epost",
                        claimsSourceMetadata
                                .findClaimMetadata("epostadresse")
                                .getDisplayName("no")),
                () -> assertEquals(
                        "Telefonnummer",
                        claimsSourceMetadata
                                .findClaimMetadata("mobiltelefonnummer")
                                .getDisplayName("no"))
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
                    "personidentifikator": "12345678901",
                    "reservasjon": "NEI",
                    "status": "AKTIV",
                    "varslingsstatus": "KAN_IKKE_VARSLES",
                    "kontaktinformasjon": 
                        {
                            "epostadresse": "test@default.digdir.no",
                            "mobiltelefonnummer": "12345678"
                        }
                
                }""";

        PersonKrr personKrr = new ObjectMapper().readValue(response, PersonKrr.class);
        when(krrIntegration.retrieve(eq(personIdentifier))).thenReturn(personKrr);
        Map<String, Object> claims = claimsSource.pull(new IssuanceTransactionId(), accessToken);
        assertAll(
                () -> assertEquals(3, claims.size()),
                () -> assertEquals(personIdentifier, claims.get("personidentifikator")),
                () -> assertEquals("12345678", claims.get("mobiltelefonnummer")),
                () -> assertEquals("test@default.digdir.no", claims.get("epostadresse")),
                () -> assertFalse(claims.containsKey("postkasseadresse"))
        );
    }
}
