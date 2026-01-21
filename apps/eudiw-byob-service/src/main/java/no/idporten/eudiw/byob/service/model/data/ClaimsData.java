package no.idporten.eudiw.byob.service.model.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.byob.service.model.Claims;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaimsData(
        @JsonProperty("path")
        String path,
        @JsonProperty("type")
        String type,
        @JsonProperty("mandatory")
        boolean mandatory,
        @JsonProperty("display")
        List<DisplayData> display
) implements Serializable {

    public ClaimsData(Claims claims) {
        this(claims.path(), claims.type(), claims.mandatory(), claims.display().stream().map(DisplayData::new).toList());
    }

    public Claims toClaims() {
        return new Claims(
                this.path,
                this.type,
                this.mandatory,
                this.display == null ? Collections.emptyList() : this.display.stream().map(DisplayData::toDisplay).toList()
        );
    }
}
