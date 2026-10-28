package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.openid4vp.verifier.model.TrustedAuthorityQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("When reading OpenID4VP configuration")
class OpenID4VPPropertiesTest {

    private static final String TRUSTED_AUTHORITY_URL =
            "https://development.trust.tech.ec.europa.eu/lists/eudiw/pid-providers.json";

    @DisplayName("When no trusted authorities are configured, then an empty list is returned")
    @Test
    void returnsEmptyListWhenTrustedAuthoritiesAreNotConfigured() {
        OpenID4VPProperties properties = new OpenID4VPProperties(" ");

        assertEquals(List.of(), properties.parseTrustedAuthorities());
    }

    @DisplayName("When valid trusted authorities are configured, then they are parsed")
    @Test
    void parsesValidTrustedAuthorities() {
        OpenID4VPProperties properties = new OpenID4VPProperties("""
                [{
                  "type": "etsi_tl",
                  "values": ["%s"]
                }]
                """.formatted(TRUSTED_AUTHORITY_URL));

        assertEquals(
                List.of(new TrustedAuthorityQuery("etsi_tl", List.of(TRUSTED_AUTHORITY_URL))),
                properties.parseTrustedAuthorities());
    }

    @DisplayName("When trusted authorities contain invalid JSON, then configuration is rejected")
    @Test
    void rejectsInvalidJson() {
        OpenID4VPProperties properties = new OpenID4VPProperties("""
                [{"type":"etsi\\_tl","values":["%s"]}]
                """.formatted(TRUSTED_AUTHORITY_URL));

        assertThrows(IllegalArgumentException.class, properties::parseTrustedAuthorities);
    }

    @DisplayName("When trusted authorities omit required fields, then configuration is rejected")
    @Test
    void rejectsMissingRequiredFields() {
        OpenID4VPProperties properties = new OpenID4VPProperties("""
                [{"type":"etsi_tl"}]
                """);

        assertThrows(IllegalArgumentException.class, properties::parseTrustedAuthorities);
    }

    @DisplayName("When trusted authorities are an empty array, then configuration is rejected")
    @Test
    void rejectsEmptyArray() {
        OpenID4VPProperties properties = new OpenID4VPProperties("[]");

        assertThrows(IllegalArgumentException.class, properties::parseTrustedAuthorities);
    }
}
