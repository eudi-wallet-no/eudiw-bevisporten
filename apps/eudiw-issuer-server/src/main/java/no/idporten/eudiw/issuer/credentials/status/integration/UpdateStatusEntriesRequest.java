package no.idporten.eudiw.issuer.credentials.status.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request object for updating status entries from the status issuer.
 */
public record UpdateStatusEntriesRequest(@JsonProperty("status_list_entries") List<UpdatedStatusEntry> statusEntries) {
}
