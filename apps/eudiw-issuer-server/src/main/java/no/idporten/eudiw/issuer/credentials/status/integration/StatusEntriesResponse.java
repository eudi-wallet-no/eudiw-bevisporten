package no.idporten.eudiw.issuer.credentials.status.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response object for allocating status entries.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StatusEntriesResponse(@JsonProperty("status_list_entries") List<StatusEntry> statusEntries) {
}
