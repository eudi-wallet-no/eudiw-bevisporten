package no.idporten.eudiw.verifier.config;

import no.idporten.eudiw.verifier.VerificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When loading verifier service properties")
class VerifierServicePropertiesTest {

    @Autowired
    private VerifierServiceProperties properties;

    @Test
    @DisplayName("binds active client applications and assigns their configured ids")
    void bindsActiveClientApplications() {
        ClientApplication clientApplication = properties.findClientApplication("junit");

        assertAll(
                () -> assertEquals("abr.vc.local", properties.getSiop2ClientId()),
                () -> assertEquals("http://localhost:9285", properties.getExternalBaseUri()),
                () -> assertEquals("junit", clientApplication.getId()),
                () -> assertEquals("junit-access", clientApplication.getKeystoreName()),
                () -> assertFalse(clientApplication.isDisabled())
        );
    }

    @Test
    @DisplayName("binds disabled client applications and rejects them when looked up")
    void rejectsDisabledClientApplications() {
        ClientApplication disabledClientApplication = properties.getClientApplications().get("junit-disabled");

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> properties.findClientApplication("junit-disabled")
        );

        assertAll(
                () -> assertEquals("junit-disabled", disabledClientApplication.getId()),
                () -> assertEquals("junit-access", disabledClientApplication.getKeystoreName()),
                () -> assertTrue(disabledClientApplication.isDisabled()),
                () -> assertEquals("invalid_request", exception.getError()),
                () -> assertEquals("Unknown client application", exception.getErrorDescription())
        );
    }

    @Test
    @DisplayName("rejects unknown client applications")
    void rejectsUnknownClientApplications() {
        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> properties.findClientApplication("unknown")
        );

        assertAll(
                () -> assertEquals("invalid_request", exception.getError()),
                () -> assertEquals("Unknown client application", exception.getErrorDescription())
        );
    }
}
