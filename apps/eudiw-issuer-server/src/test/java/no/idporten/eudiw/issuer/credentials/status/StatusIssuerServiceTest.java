package no.idporten.eudiw.issuer.credentials.status;

import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusIssuerIntegration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@DisplayName("When allocation a status on the status list")
@ExtendWith(MockitoExtension.class)
public class StatusIssuerServiceTest {

    @Mock
    StatusIssuerIntegration statusIssuerIntegration;

    @DisplayName("then the status list feature is feature switched")
    @Test
    void testFeatureSwitch() {
        StatusIssuerProperties properties = new StatusIssuerProperties();
        StatusIssuerService service = new StatusIssuerService(properties, null);
        assertFalse(service.isEnabled());
        properties.setEnabled(true);
        assertTrue(service.isEnabled());
    }

    @DisplayName("then status entries is allocated by integrating with the status issuer")
    @Test
    void testAllocateStatus() {
        StatusIssuerService service = new StatusIssuerService(null, statusIssuerIntegration);
        when(statusIssuerIntegration.allocateStatusEntries(anyInt()))
                .thenAnswer(invocationOnMock -> IntStream.range(0, invocationOnMock.getArgument(0))
                        .mapToObj(idx -> new StatusEntry(idx, URI.create("https://junit.eidas2sandkasse.dev/lists/" + idx * 2)))
                        .toList());
        List<CredentialStatus> credentialStatus = service.allocateStatus(2);
        assertAll(
                () -> assertEquals(2, credentialStatus.size()),
                () -> assertEquals(0, credentialStatus.getFirst().statusList().index()),
                () -> assertEquals("https://junit.eidas2sandkasse.dev/lists/0", credentialStatus.getFirst().statusList().uri().toString()),
                () -> assertEquals(1, credentialStatus.getLast().statusList().index()),
                () -> assertEquals("https://junit.eidas2sandkasse.dev/lists/2", credentialStatus.getLast().statusList().uri().toString())
        );
    }

}
