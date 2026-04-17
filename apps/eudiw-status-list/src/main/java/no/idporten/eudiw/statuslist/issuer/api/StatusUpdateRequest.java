package no.idporten.eudiw.statuslist.issuer.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StatusUpdateRequest(@JsonProperty("status_list_entries") @NotEmpty @Valid List<StatusEntryUpdateRequest> statusListEntries) {

}
