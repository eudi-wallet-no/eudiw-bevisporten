package no.idporten.eudiw.issuer.claimssource.byob.domain;

import lombok.Builder;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;

import java.util.List;

import static no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata.TYPE_STRING;

@Builder
public record DynamicClaimMetadata(
        String path,
        // TODO type/hierarki/struktur
        String type, // String, Number, Boolean, Binary, FullDate, DateTime, List, Map
        List<DocumentMetadata.Display> display,
        boolean mandatory,
        String validationRegex
)
{
    public DynamicClaimMetadata(String path, List<DocumentMetadata.Display> display, boolean mandatory, String validationRegex) {
        this(path, TYPE_STRING, display, mandatory, validationRegex);
    }

}
