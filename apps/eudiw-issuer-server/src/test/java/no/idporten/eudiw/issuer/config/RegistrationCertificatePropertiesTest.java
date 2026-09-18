package no.idporten.eudiw.issuer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.OriginTrackedMapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When configuring a registration certificate")
class RegistrationCertificatePropertiesTest {

    @DisplayName("then all configured values are available")
    @Test
    void configuredValuesAreAvailable() {
        OriginTrackedMapPropertySource propertySource = new OriginTrackedMapPropertySource(
                "test",
                Map.of(
                        "tenant.registration-certificate.enabled", true,
                        "tenant.registration-certificate.registration-certificate-jwt", "registration-certificate-jwt",
                        "tenant.registration-certificate.registrar-dataset", "registrar-dataset"));
        CredentialIssuerTenant tenant = new Binder(ConfigurationPropertySources.from(propertySource))
                .bind("tenant", Bindable.of(CredentialIssuerTenant.class))
                .orElseThrow(IllegalStateException::new);
        RegistrationCertificateProperties properties = tenant.getRegistrationCertificate();

        assertAll(
                () -> assertTrue(properties.enabled()),
                () -> assertEquals("registration-certificate-jwt", properties.registrationCertificateJwt()),
                () -> assertEquals("registrar-dataset", properties.registrarDataset()));
    }

    @DisplayName("then a JSON registrar dataset is returned as an object")
    @Test
    void registrarDatasetIsReturnedAsJsonObject() {
        RegistrationCertificateProperties properties = new RegistrationCertificateProperties(
                true,
                "registration-certificate-jwt",
                "{\"organization\":\"Digdir\"}");

        assertEquals("Digdir", properties.registrarDatasetJson().get("organization").asText());
    }

    @DisplayName("then a non-object registrar dataset is rejected")
    @Test
    void nonObjectRegistrarDatasetIsRejected() {
        RegistrationCertificateProperties properties = new RegistrationCertificateProperties(
                true,
                "registration-certificate-jwt",
                "[]");

        assertThrows(IllegalArgumentException.class, properties::registrarDatasetJson);
    }

    @DisplayName("then a disabled registration certificate does not parse the registrar dataset")
    @Test
    void disabledRegistrationCertificateDoesNotParseRegistrarDataset() {
        RegistrationCertificateProperties properties = new RegistrationCertificateProperties(
                false,
                "registration-certificate-jwt",
                "not valid json");

        assertNull(properties.registrarDatasetJson());
    }

    @DisplayName("then validation passes when enabled with a JWT and a valid JSON registrar dataset")
    @Test
    void validationPassesWhenEnabledWithValidValues() {
        RegistrationCertificateProperties properties = new RegistrationCertificateProperties(
                true,
                "registration-certificate-jwt",
                "{\"organization\":\"Digdir\"}");

        assertDoesNotThrow(properties::validate);
    }

    @DisplayName("then validation fails when enabled without a registration certificate JWT")
    @Test
    void validationFailsWhenEnabledWithoutJwt() {
        RegistrationCertificateProperties properties = new RegistrationCertificateProperties(
                true,
                null,
                "{\"organization\":\"Digdir\"}");

        assertThrows(IllegalArgumentException.class, properties::validate);
    }

    @DisplayName("then validation fails when enabled without a registrar dataset")
    @Test
    void validationFailsWhenEnabledWithoutRegistrarDataset() {
        RegistrationCertificateProperties properties = new RegistrationCertificateProperties(
                true,
                "registration-certificate-jwt",
                null);

        assertThrows(IllegalArgumentException.class, properties::validate);
    }

    @DisplayName("then validation fails when enabled with an invalid registrar dataset")
    @Test
    void validationFailsWhenEnabledWithInvalidRegistrarDataset() {
        RegistrationCertificateProperties properties = new RegistrationCertificateProperties(
                true,
                "registration-certificate-jwt",
                "not valid json");

        assertThrows(IllegalArgumentException.class, properties::validate);
    }

    @DisplayName("then validation passes when disabled regardless of missing or invalid values")
    @Test
    void validationPassesWhenDisabled() {
        RegistrationCertificateProperties properties = new RegistrationCertificateProperties(
                false,
                null,
                null);

        assertDoesNotThrow(properties::validate);
    }
}
