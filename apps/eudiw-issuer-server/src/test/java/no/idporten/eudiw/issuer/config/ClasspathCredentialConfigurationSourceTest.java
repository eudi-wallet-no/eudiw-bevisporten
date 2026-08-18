package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClasspathCredentialConfigurationSourceTest {

    private CredentialConfigurationSource credentialConfigurationSource;

    @BeforeEach
    void setUp() {
        credentialConfigurationSource = new ClasspathCredentialConfigurationSource(new LocalResourceProperties(List.of(URI.create("classpath:credential-configurations/junit_mso_mdoc.json"))));
    }

    @DisplayName("then uninitialized will return empty list")
    @Test
    void testRetrieveUninitialized() {
        List<ExtendedCredentialConfiguration> retrievedCredentialConfigurations = credentialConfigurationSource.retrieve();
        assertAll(
                () -> assertNotNull(retrievedCredentialConfigurations),
                () -> assertTrue(retrievedCredentialConfigurations.isEmpty())
        );
    }

    @DisplayName("then initialized return JSON file contents as credential configuration")
    @Test
    void testRetrieveFromClasspath() {
        credentialConfigurationSource.init();
        List<ExtendedCredentialConfiguration> retrievedCredentialConfigurations = credentialConfigurationSource.retrieve();
        assertAll(
                () -> assertNotNull(retrievedCredentialConfigurations),
                () -> assertEquals(1, retrievedCredentialConfigurations.size()),
                () -> assertEquals("junitdoc_mso_mdoc", retrievedCredentialConfigurations.getFirst().getCredentialConfigurationId())
        );
    }

}
