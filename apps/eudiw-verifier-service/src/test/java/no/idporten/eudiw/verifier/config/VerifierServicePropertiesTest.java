package no.idporten.eudiw.verifier.config;

import no.idporten.eudiw.verifier.VerificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When loading verifier service properties")
class VerifierServicePropertiesTest {

    @Autowired
    private VerifierServiceProperties properties;

    @Nested
    @DisplayName("testing client application lookup")
    class FindClientApplication {
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
        @DisplayName("rejects unknown client applications and return VerificationException")
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

    @Nested
    @DisplayName("validate client application and api-key")
    class ValidateClientApplication {
        @Test
        @DisplayName("with valid clientApplication and api-key should return status OK and valid properties for client application")
        void validateActiveClientApplications() {
            ClientApplication clientApplication = properties.validateClientApplication("junit", "not-hidden-api-key");

            assertAll(
                    () -> assertEquals("abr.vc.local", properties.getSiop2ClientId()),
                    () -> assertEquals("http://localhost:9285", properties.getExternalBaseUri()),
                    () -> assertEquals("junit", clientApplication.getId()),
                    () -> assertEquals("junit-access", clientApplication.getKeystoreName()),
                    () -> assertFalse(clientApplication.isDisabled())
            );
        }

        @ParameterizedTest
        @DisplayName("rejects invalid api-key for known client applications with UNAUTHORIZED")
        @ValueSource(strings = {"", " ", "invalid-api-key"})
        void rejectsInvalidApiKeyForValidClientApplications(final String apiKey) {

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> properties.validateClientApplication("junit", apiKey)
            );

            assertAll(
                    () -> assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode()),
                    () -> assertEquals("Invalid API Key", exception.getReason())
            );
        }

        @Test
        @DisplayName("rejects valid api-key for unknown client applications with UNAUTHORIZED")
        void rejectsUnknownClientApplicationsWithValidApiKey() {

            VerificationException exception = assertThrows(
                    VerificationException.class,
                    () -> properties.validateClientApplication("unknown", "not-hidden-api-key")
            );

            assertAll(
                    () -> assertEquals("invalid_request", exception.getError()),
                    () -> assertEquals("Unknown client application", exception.getErrorDescription())
            );
        }
    }
}
