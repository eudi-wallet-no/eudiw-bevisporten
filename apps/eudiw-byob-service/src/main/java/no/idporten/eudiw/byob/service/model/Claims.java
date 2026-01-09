package no.idporten.eudiw.byob.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Claims(
        @NotNull
        @JsonProperty("path")
        String path,
        @JsonProperty("mandatory")
        boolean mandatory,
        @JsonProperty("display")
        List<Display> display
) {
}
