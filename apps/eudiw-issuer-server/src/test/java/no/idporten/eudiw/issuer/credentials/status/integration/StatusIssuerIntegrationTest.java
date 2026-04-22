package no.idporten.eudiw.issuer.credentials.status.integration;

import no.idporten.eudiw.issuer.claimssource.exception.CredentialRequestDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("When interacting with status issuer")
@ActiveProfiles("junit")
@SpringBootTest
public class StatusIssuerIntegrationTest {

    @Autowired
    private StatusIssuerIntegration statusIssuerIntegration;

    private MockServerRestClientCustomizer customizer;

    @BeforeEach
    public void setUp() {
        customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);
        statusIssuerIntegration.setRestClient(builder.build());
    }

    @DisplayName("to allocate status entries")
    @Nested
    class AllocationTests {

        @DisplayName("then a status allocation request containing number of entries is sent and status entries are returned")
        @Test
        void testAllocateStatusEntries() {
            final String response = """
                    {
                      "status_list_entries": [
                        {
                          "idx": 0,
                          "uri": "https://junit.status.eidas2sandkasse.dev/lists/1"
                        },
                        {
                          "idx": 56,
                          "uri": "https://junit.status.eidas2sandkasse.dev/lists/1"
                        }
                      ]
                    }""";
            customizer.getServer()
                    .expect(requestTo("/status-issuer/api/v1/entries"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(jsonPath("$.number_of_entries").value(2))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
            List<StatusEntry> statusEntries = statusIssuerIntegration.allocateStatusEntries(2);
            customizer.getServer().verify();
            assertAll(
                    () -> assertEquals(2, statusEntries.size()),
                    () -> assertEquals(0, statusEntries.getFirst().idx()),
                    () -> assertEquals("https://junit.status.eidas2sandkasse.dev/lists/1", statusEntries.getFirst().uri().toString()),
                    () -> assertEquals(56, statusEntries.getLast().idx()),
                    () -> assertEquals("https://junit.status.eidas2sandkasse.dev/lists/1", statusEntries.getLast().uri().toString())
            );
        }


        @DisplayName("then error responses will deny credential issuance")
        @Test
        void test4xxResponseGivesCredentialRequestDeniedException() {
            final String errorResponse = """
                    {
                        "error": "invalid_something",
                        "error_description": "Something"
                    }""";
            customizer.getServer()
                    .expect(requestTo("/status-issuer/api/v1/entries"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(jsonPath("$.number_of_entries").value(100))
                    .andRespond(
                            withStatus(HttpStatus.BAD_REQUEST)
                                    .body(errorResponse).
                                    contentType(MediaType.APPLICATION_JSON));
            CredentialRequestDeniedException e = assertThrows(
                    CredentialRequestDeniedException.class,
                    () -> statusIssuerIntegration.allocateStatusEntries(100));
            customizer.getServer().verify();
            assertAll(
                    () -> assertEquals("credential_request_denied", e.getError()),
                    () -> assertTrue(e.getMessage().contains("Failed to allocate status entry from status issuer"))
            );
        }

    }

    @DisplayName("to update status entries")
    @Nested
    class UpdateStatusEntriesTests {
        @DisplayName("then a status update request for status entries is sent")
        @Test
        void testUpdateStatusEntries() {
            List<UpdatedStatusEntry> statusEntries = List.of(
                    new UpdatedStatusEntry(1, URI.create("https://junit.status.eidas2sandkasse.dev/lists/1"), "INVALID"),
                    new UpdatedStatusEntry(56, URI.create("https://junit.status.eidas2sandkasse.dev/lists/3"), "INVALID")
            );
            customizer.getServer()
                    .expect(requestTo("/status-issuer/api/v1/entries"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(jsonPath("$.status_list_entries", hasSize(2)))
                    .andExpect(jsonPath("$.status_list_entries[0].idx").value(1))
                    .andExpect(jsonPath("$.status_list_entries[0].uri").value("https://junit.status.eidas2sandkasse.dev/lists/1"))
                    .andExpect(jsonPath("$.status_list_entries[0].status_type").value("INVALID"))
                    .andExpect(jsonPath("$.status_list_entries[1].idx").value(56))
                    .andExpect(jsonPath("$.status_list_entries[1].uri").value("https://junit.status.eidas2sandkasse.dev/lists/3"))
                    .andExpect(jsonPath("$.status_list_entries[1].status_type").value("INVALID"))
                    .andRespond(withNoContent());
            statusIssuerIntegration.updateStatusEntries(statusEntries);
            customizer.getServer().verify();
        }
    }

}
