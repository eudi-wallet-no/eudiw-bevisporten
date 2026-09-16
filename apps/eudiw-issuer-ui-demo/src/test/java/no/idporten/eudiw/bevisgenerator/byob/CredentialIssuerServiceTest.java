package no.idporten.eudiw.bevisgenerator.byob;

import no.idporten.eudiw.bevisgenerator.integration.byobservice.ByobService;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.CredentialDefinitionFactory;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialDefinition;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialMetadata;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.Display;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.ExampleCredentialData;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CredentialIssuerServiceTest {

    @Test
    void mapsDynamicCredentialsToTheRegisteredMaskinportenScope() {
        ByobService byobService = mock(ByobService.class);
        CredentialDefinition definition = mock(CredentialDefinition.class);
        when(definition.getCredentialConfigurationId()).thenReturn("dynamic-credential");
        when(definition.getExampleCredentialData()).thenReturn(new ExampleCredentialData());
        when(definition.getCredentialMetadata()).thenReturn(
                new CredentialMetadata(List.of(new Display("Testbevis")), List.of())
        );
        when(byobService.getByCredentialConfigurationId("dynamic-credential")).thenReturn(definition);

        CredentialIssuerService service = new CredentialIssuerService(
                byobService,
                new IssuerServerProperties("http://issuer", "/issuance", null, null, null),
                new ObjectMapper()
        );

        CredentialConfiguration configuration = service.getCredentialConfigurationById("dynamic-credential");

        assertEquals(CredentialDefinitionFactory.DYNAMIC_CREDENTIAL_SCOPE, configuration.scope());
    }
}
