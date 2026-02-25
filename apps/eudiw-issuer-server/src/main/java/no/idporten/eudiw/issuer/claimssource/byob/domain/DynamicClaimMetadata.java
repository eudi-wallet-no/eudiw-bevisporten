package no.idporten.eudiw.issuer.claimssource.byob.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.List;

@Builder
public record DynamicClaimMetadata(
        String path,
        String type, // string, number, boolean, binary, iso_date, iso_datetime, list, map
        @JsonProperty("mime_type")
        String mimeType, // only for type = binary
        List<Display> display,
        boolean mandatory,
        String validationRegex
)
{
    public DynamicClaimMetadata(String path, List<Display> display, boolean mandatory, String validationRegex) {
        this(path, ClaimDataType.STRING.name(), null, display, mandatory, validationRegex);
    }

}
