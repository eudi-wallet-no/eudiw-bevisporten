package no.idporten.eudiw.issuer.claimssource.byob.domain;

import lombok.Builder;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;

import java.util.List;
import java.util.stream.Collectors;

import static no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata.TYPE_STRING;

@Builder
public record DynamicCredentialMetadata(List<DocumentMetadata.Display> display,
                                        List<DynamicClaimMetadata> claims) {


    public static String defaultValidationRegexString() {
        return "^[\\x20-\\x7EæøåÆØÅ]{1,255}$";
    }

    public DocumentMetadata convertToDocumentMetadata() {

        if (claims == null) {
            return new DocumentMetadata(display, null);
        }

        List<ClaimMetadata> claimsConverted = claims.stream()
                .map(claim -> new ClaimMetadata(
                        claim.path(),
                        TYPE_STRING,
                        claim.display().stream().collect(Collectors.toMap(DocumentMetadata.Display::locale, DocumentMetadata.Display::name)),
                        claim.mandatory(),
                        claim.validationRegex() == null ? defaultValidationRegexString() : claim.validationRegex()
                ))
                .toList();

        return new DocumentMetadata(
                display,
                claimsConverted
        );
    }
}
