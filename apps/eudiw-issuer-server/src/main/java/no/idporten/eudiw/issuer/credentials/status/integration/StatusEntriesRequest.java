package no.idporten.eudiw.issuer.credentials.status.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request object for allocating a number of status entries from the status issuer.
 */
public record StatusEntriesRequest(@JsonProperty("number_of_entries") int numberOfEntries) {
}
