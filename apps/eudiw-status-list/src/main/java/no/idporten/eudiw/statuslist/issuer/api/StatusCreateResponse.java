package no.idporten.eudiw.statuslist.issuer.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.statuslist.domain.StatusEntry;

import java.util.List;

public record StatusCreateResponse(@JsonProperty("status_list_entries") List<StatusEntry> statusListEntries) {

}
