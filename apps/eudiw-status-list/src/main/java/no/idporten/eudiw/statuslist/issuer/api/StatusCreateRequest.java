package no.idporten.eudiw.statuslist.issuer.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatusCreateRequest(
        @JsonProperty("number_of_entries") @Min(1) int numberOfEntries) {
}
