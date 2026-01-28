package no.idporten.eudiw.issuer.claimssource.byob.domain;

import lombok.Builder;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record DynamicCredentialMetadata(List<DocumentMetadata.Display> display,
                                        List<DynamicClaimMetadata> claims) {

    // Flytt ut regex på neste sak + lag enum for type
    private static String getDefaultStringValidationRegex() {
        return "^[\\x20-\\x7EæøåÆØÅ]{1,255}$";
    }

    private static String getDefaultBinaryValidationRegex() {
        return "^[-A-Za-z0-9+/]*={0,3}$";
    }

    private String getDefaultValidationRegex(String type) {

        return switch (type) {
            case ClaimMetadata.TYPE_STRING -> getDefaultStringValidationRegex();
            case ClaimMetadata.TYPE_BINARY -> getDefaultBinaryValidationRegex();
            case null, default -> getDefaultStringValidationRegex();
        };
    }

    // Eksplisitt konvertering for å unngå feil ved utvidelser i fremtiden og beholde String som default.
    private String convertType(String type) {
        return switch (type) {
            case ClaimMetadata.TYPE_STRING -> ClaimMetadata.TYPE_STRING;
            case ClaimMetadata.TYPE_BINARY -> ClaimMetadata.TYPE_BINARY;
            case ClaimMetadata.TYPE_BOOLEAN -> ClaimMetadata.TYPE_BOOLEAN;
            case ClaimMetadata.TYPE_NUMBER -> ClaimMetadata.TYPE_NUMBER;
            case ClaimMetadata.TYPE_FULLDATE -> ClaimMetadata.TYPE_FULLDATE;
            case ClaimMetadata.TYPE_DATETIME -> ClaimMetadata.TYPE_DATETIME;
            case ClaimMetadata.TYPE_LIST -> ClaimMetadata.TYPE_LIST;
            case ClaimMetadata.TYPE_MAP -> ClaimMetadata.TYPE_MAP;
            case null, default -> ClaimMetadata.TYPE_STRING;
        };
    }


    public DocumentMetadata convertToDocumentMetadata() {

        if (claims == null) {
            return new DocumentMetadata(display, null);
        }

        List<ClaimMetadata> claimsConverted = claims.stream()
                .map(claim -> new ClaimMetadata(
                        ClaimMetadata.EMPTY_NAMESPACE, // namespace only for mdoc, byob only supports SD-JWT-VC
                        claim.path(),       // path as single string
                        convertType(claim.type()),
                        claim.mimeType(),
                        claim.display().stream().collect(Collectors.toMap(DocumentMetadata.Display::locale, DocumentMetadata.Display::name)),
                        claim.mandatory(),
                        claim.validationRegex() == null ? getDefaultValidationRegex(claim.type()) : claim.validationRegex()
                ))
                .toList();

        return new DocumentMetadata(
                display,
                claimsConverted
        );
    }
}
