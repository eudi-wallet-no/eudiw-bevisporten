package no.idporten.eudiw.issuer.claimssource.byob.domain;

import lombok.Builder;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record DynamicCredentialMetadata(List<DocumentMetadata.Display> display,
                                        List<DynamicClaimMetadata> claims) {


    private static String getDefaultStringValidationRegex() {
        return "^[\\x20-\\x7EæøåÆØÅ]{1,255}$";
    }

    private static String getDefaultBinaryValidationRegex() {
        return "^[-A-Za-z0-9+/]*={0,3}$";
    }

    private String getDefaultValidationRegex(String type) {

        return switch (type) {
            case ClaimMetadata.TYPE_STRING -> getDefaultStringValidationRegex();
            case ClaimMetadata.TYPE_CUSTOM_BILDE, ClaimMetadata.TYPE_BINARY -> getDefaultBinaryValidationRegex();
            case null, default -> getDefaultStringValidationRegex();
        };
    }

    private String convertType(String type) {
        return switch (type) {
            case ClaimMetadata.TYPE_CUSTOM_BILDE -> ClaimMetadata.TYPE_BINARY;
            case null, default -> type;
        };
    }


    public DocumentMetadata convertToDocumentMetadata() {

        if (claims == null) {
            return new DocumentMetadata(display, null);
        }

        List<ClaimMetadata> claimsConverted = claims.stream()
                .map(claim -> new ClaimMetadata(
                        claim.path(),
                        convertType(claim.type()),
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
