package no.idporten.eudiw.issuer.claimssource.byob.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;

import java.util.List;

import static no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata.TYPE_STRING;

@Builder
public record DynamicClaimMetadata(
        String path,
        String type, // string, number, boolean, binary, iso_date, iso_datetime, list, map
        @JsonProperty("mime_type")
        String mimeType, // only for type = binary
        List<DocumentMetadata.Display> display,
        boolean mandatory,
        String validationRegex
)
{
    public DynamicClaimMetadata(String path, List<DocumentMetadata.Display> display, boolean mandatory, String validationRegex) {
        this(path, TYPE_STRING, null, display, mandatory, validationRegex);
    }

}
