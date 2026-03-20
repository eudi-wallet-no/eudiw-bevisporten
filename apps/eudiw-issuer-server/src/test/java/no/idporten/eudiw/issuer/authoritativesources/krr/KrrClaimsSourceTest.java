package no.idporten.eudiw.issuer.authoritativesources.krr;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.authoritativesources.krr.model.PersonKrr;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.Map;

import static no.idporten.eudiw.issuer.TestData.accessToken;
import static no.idporten.eudiw.issuer.TestData.junitIssuerTenant;
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

    @DisplayName("then push is not supported")
    @Test
    void testPushNotSupported() {
        assertThrows(IssuerServerException.class, () -> claimsSource.push(null, new CredentialData(Collections.emptyMap())));
    }

    @DisplayName("then data can be pulled from authoritative source")
    @Test
    void testPullFromAuthoritativeSource() throws Exception {
        String personIdentifier = "12345678901";
        JWT accessToken = accessToken(personIdentifier);
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

        PersonKrr personKrr = new JsonMapper().readValue(response, PersonKrr.class);
        when(krrIntegration.retrieve(eq(personIdentifier))).thenReturn(personKrr);
        CredentialData data = claimsSource.pull(new PreAuthorizedIssuanceContext(junitIssuerTenant(), null, new IssuanceTransactionId(), accessToken));
        assertNotNull(data);
        Map<String, Object> claims = data.claims();
        assertAll(
                () -> assertNotNull(claims),
                () -> assertEquals(3, claims.size()),
                () -> assertEquals(personIdentifier, claims.get("personidentifikator")),
                () -> assertEquals("12345678", claims.get("mobiltelefonnummer")),
                () -> assertEquals("test@default.digdir.no", claims.get("epostadresse")),
                () -> assertFalse(claims.containsKey("postkasseadresse"))
        );
    }
}
