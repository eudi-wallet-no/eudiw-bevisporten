package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.exception.CredentialRequestDeniedException;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.claimssource.exception.InvalidCredentialDataException;
import no.idporten.eudiw.issuer.credentials.ClaimDataTypeValidator;
import no.idporten.eudiw.issuer.credentials.ClaimValueConverter;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.credentials.types.Claim;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractClaimsSource {

    private final ClaimValueConverter claimValueConverter = new ClaimValueConverter();
    private final ClaimDataTypeValidator claimDataTypeValidator = new ClaimDataTypeValidator();

    public final CredentialData validate(ExtendedCredentialMetadata credentialMetadata, CredentialData credentialData) {
        if (credentialMetadata == null) {
            throw new IssuerServerException(ErrorCode.INVALID_REQUEST, "credential metadata null in request.");
        }
        Map<String, Object> claims = credentialData.claims();
        for (ExtendedClaimsDescription extendedClaimsDescription : credentialMetadata.claims()) {
            validateClaim(extendedClaimsDescription, claims);
        }
        // reject all unknown claims
        for (String claimName : claims.keySet()) {
            if (credentialMetadata.findClaimMetadata(claimName) == null) {
                throw new InvalidCredentialDataException("Unsupported claims in request.");
            }
        }
        return new CredentialData(Collections.unmodifiableMap(claims));
    }

    protected final void validateClaim(ExtendedClaimsDescription extendedClaimsDescription, Map<String, Object> claims) {
        if (extendedClaimsDescription.mandatory() && !claims.containsKey(extendedClaimsDescription.name())) {
            throw new InvalidCredentialDataException("Missing required claim [%s]".formatted(extendedClaimsDescription.name()));
        }
        claimDataTypeValidator.validate(extendedClaimsDescription, claims.get(extendedClaimsDescription.name()));
    }

    protected List<Claim> convertCredentialDataToClaims(CredentialIssueContext credentialIssueContext, Map<String, Object> storedClaims) {
        ExtendedCredentialMetadata extendedCredentialMetadata = credentialIssueContext.credentialConfiguration().getExtendedCredentialMetadata();
        try {
            return storedClaims.keySet().stream()
                    .map(extendedCredentialMetadata::findClaimMetadata)
                    .filter(Objects::nonNull)
                    .map(claimMetadata -> getClaim(claimMetadata, storedClaims)).toList();
        } catch (InvalidCredentialDataException e) {
            throw new CredentialRequestDeniedException(credentialIssueContext.credentialConfiguration().getCredentialConfigurationId(), e.getErrorDescription(), e);
        }
    }

    public Claim getClaim(ExtendedClaimsDescription claim, Map<String, Object> storedClaims) {
        return claimValueConverter.convertClaim(claim, storedClaims);
    }

}
