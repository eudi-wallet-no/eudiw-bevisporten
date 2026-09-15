package no.idporten.eudiw.bevisgenerator.integration.issuerserver.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CredentialConfigurationBindingTest {

    @Test
    void bindsLocalIdAlongsideCredentialConfigurationId() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of(
                        "credential-configurations[0].local-id", "webuild-pid-mdoc",
                        "credential-configurations[0].credential-configuration-id",
                        "no.digdir.eudiw.pid_mso_mdoc",
                        "credential-configurations[0].credential-issuer",
                        "https://issuer.example/webuild",
                        "credential-configurations[0].scope", "eudiw:no:pid",
                        "credential-configurations[0].description", "Webuild PID",
                        "credential-configurations[0].json-request", "{}"
                )
        ));

        CredentialConfiguration configuration = Binder.get(environment)
                .bind("credential-configurations[0]", Bindable.of(CredentialConfiguration.class))
                .orElseThrow(() -> new AssertionError("Expected credential configuration to bind"));

        assertEquals("webuild-pid-mdoc", configuration.localId());
        assertEquals("webuild-pid-mdoc", configuration.selectionId());
        assertEquals("no.digdir.eudiw.pid_mso_mdoc", configuration.credentialConfigurationId());
        assertEquals("https://issuer.example/webuild", configuration.credentialIssuer());
    }
}
