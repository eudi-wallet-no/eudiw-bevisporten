package no.idporten.eudiw.issuer.claimssource.byob.domain;

import lombok.Builder;
import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.credentials.types.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.types.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Builder
public record DynamicCredentialMetadata(List<Display> display,
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


    public ExtendedCredentialMetadata toExtendedCredentialMetadata(DynamicCredentialConfiguration dynamicCredentialConfiguration) {
        if (claims == null) {
            return new ExtendedCredentialMetadata(display, null);
        }

        List<ExtendedClaimsDescription> claimsConverted = claims.stream()
                .map(claim -> convertToExtendedClaimsDescription(claim, dynamicCredentialConfiguration))
                .toList();

        return new ExtendedCredentialMetadata(display, claimsConverted);
    }

    @NotNull
    private ExtendedClaimsDescription convertToExtendedClaimsDescription(DynamicClaimMetadata claim, DynamicCredentialConfiguration dynamicCredentialConfiguration) {
        ClaimDataType type =convertType(claim.type());
        return new ExtendedClaimsDescription(
                CredentialFormat.fromString(dynamicCredentialConfiguration.format()) == CredentialFormat.SD_JWT_VC
                        ? ExtendedClaimsDescription.EMPTY_NAMESPACE
                        : dynamicCredentialConfiguration.credentialType(),
                claim.path(),       // path as single string
                type,
                claim.mimeType(),
                claim.display(),
                claim.mandatory(),
                claim.validationRegex() != null ? claim.validationRegex() : type.getDefaultRegex());
    }
}
