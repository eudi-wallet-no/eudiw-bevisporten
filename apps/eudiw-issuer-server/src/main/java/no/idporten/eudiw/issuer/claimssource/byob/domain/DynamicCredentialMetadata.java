package no.idporten.eudiw.issuer.claimssource.byob.domain;

import lombok.Builder;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimDataType;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record DynamicCredentialMetadata(List<DocumentMetadata.Display> display,
                                        List<DynamicClaimMetadata> claims) {


    // Eksplisitt konvertering for å unngå feil ved utvidelser i fremtiden og beholde String som default.
    private ClaimDataType convertType(String type) {
        if (type == null || type.isEmpty()) {
            return ClaimDataType.STRING;
        }
        //tmp fix until bilde is deleted from redis in systest
        if (type.equals("bilde")) {
            return ClaimDataType.BINARY;
        }
        return ClaimDataType.valueOfCaseInsensitive(type);
    }


    public DocumentMetadata convertToDocumentMetadata() {
        if (claims == null) {
            return new DocumentMetadata(display, null);
        }

        List<ClaimMetadata> claimsConverted = claims.stream()
                .map(this::convertToClaimMetadata)
                .toList();

        return new DocumentMetadata(display, claimsConverted);
    }

    @NotNull
    private ClaimMetadata convertToClaimMetadata(DynamicClaimMetadata claim) {
        ClaimDataType type = convertType(claim.type());
        return new ClaimMetadata(
                ClaimMetadata.EMPTY_NAMESPACE, // namespace only for mdoc, byob only supports SD-JWT-VC
                claim.path(),       // path as single string
                type,
                claim.mimeType(),
                claim.display().stream().collect(Collectors.toMap(DocumentMetadata.Display::locale, DocumentMetadata.Display::name)),
                claim.mandatory(),
                claim.validationRegex() != null ? claim.validationRegex() : type.getDefaultRegex()
        );
    }
}
