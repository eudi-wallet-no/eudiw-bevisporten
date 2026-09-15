package no.idporten.eudiw.bevisgenerator.integration.issuerserver.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IssuerServerPropertiesTest {

    @Test
    void findsConfigurationsByLocalSelectionIdWhenCredentialConfigurationIdsAreShared() {
        CredentialConfiguration webuildConfiguration = new CredentialConfiguration(
                "https://issuer.example/webuild",
                "no.digdir.eudiw.pid_mso_mdoc",
                "eudiw:no:pid",
                null,
                "Webuild PID",
                "{}",
                "webuild-pid-mdoc"
        );
        CredentialConfiguration pidConfiguration = new CredentialConfiguration(
                "https://issuer.example/pid",
                "no.digdir.eudiw.pid_mso_mdoc",
                "eudiw:no:pid",
                null,
                "PID push",
                "{}",
                "pid-mdoc-push"
        );
        IssuerServerProperties properties = new IssuerServerProperties(
                "https://issuer.example/bevisgenerator",
                "/api/v1/credential/issuance-transaction",
                List.of(webuildConfiguration, pidConfiguration),
                List.of(),
                List.of(),
                List.of()
        );

        assertEquals(webuildConfiguration, properties.findCredentialConfiguration("webuild-pid-mdoc"));
        assertEquals(pidConfiguration, properties.findCredentialConfiguration("pid-mdoc-push"));
        assertEquals(
                webuildConfiguration,
                properties.findCredentialConfiguration(
                        "https://issuer.example/webuild",
                        "no.digdir.eudiw.pid_mso_mdoc"
                )
        );
        assertEquals(
                pidConfiguration,
                properties.findCredentialConfiguration(
                        "https://issuer.example/pid",
                        "no.digdir.eudiw.pid_mso_mdoc"
                )
        );
    }
}
