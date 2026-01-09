package no.idporten.eudiw.issuer.claimssource.byob.domain;

import lombok.Builder;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;

import java.util.List;

@Builder
public record DynamicClaimMetadata(
        String path,
        List<DocumentMetadata.Display> display,
        boolean mandatory,
        String validationRegex
)
{

}
