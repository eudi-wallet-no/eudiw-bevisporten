package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.cache.ClaimsSourceCache;
import no.idporten.eudiw.issuer.credentials.ClaimDataTypeValidator;
import no.idporten.eudiw.issuer.credentials.ClaimValueConverter;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.credentials.types.ClaimMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceFormatException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.text.ParseException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract non-sealed class AbstractPreAuthorizedClaimsSource implements PreAuthorizedClaimsSource {

    private ClaimsSourceCache cache;
    private final ClaimValueConverter claimValueConverter = new ClaimValueConverter();
    private final ClaimDataTypeValidator claimDataTypeValidator = new ClaimDataTypeValidator();

    @Autowired
    public void setClaimsSourceCache(ClaimsSourceCache claimsSourceCache) {
        this.cache = claimsSourceCache;
    }

    @Override
    public final CredentialData validate(DocumentMetadata credentialMetadata, CredentialData credentialData) {
        if (credentialMetadata == null) {
            throw new IssuerServerException("invalid_request", "credentialMetadata null in request.", HttpStatus.BAD_REQUEST);
        }
        CredentialMetadataContext credentialMetadataContext = new CredentialMetadataContext(credentialData.credentialConfigurationId(), null, null);
        return validate(credentialMetadata, credentialData, credentialMetadataContext);
    }

    @NotNull
    private CredentialData validate(DocumentMetadata credentialMetadata, CredentialData credentialData, CredentialMetadataContext credentialMetadataContext) {

        Map<String, Object> claims = credentialData.claims();
        for (ClaimMetadata claimMetadata : credentialMetadata.claims()) {
            validateClaim(claimMetadata, claims);
        }

        // reject all unknown claims
        for (String claimName : claims.keySet()) {
            if (getDocumentMetadata(credentialMetadataContext).findClaimMetadata(claimName) == null) {
                throw new IssuerServerException("invalid_request", "Unsupported claims in request.", HttpStatus.BAD_REQUEST);
            }
        }
        return new CredentialData(Collections.unmodifiableMap(claims), credentialData.credentialConfigurationId());
    }

    protected final void validateClaim(ClaimMetadata claimMetadata, Map<String, Object> claims) {
        if (claimMetadata.mandatory() && !claims.containsKey(claimMetadata.name())) {
            throw new IssuerServerException("invalid_request", "Missing required claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        claimDataTypeValidator.validate(claimMetadata, claims.get(claimMetadata.name()));
    }

    @Override
    public IssuanceTransactionId store(IssuanceTransactionId issuanceTransactionId, final Map<String, Object> claims, Duration lifetime) {
        cache.storeClaims(issuanceTransactionId, claims, lifetime);
        return issuanceTransactionId;
    }

    @Override
    public final List<Claim> issueClaims(CredentialIssueContext credentialIssueContext) {
        final String transactionId;
        try {
            transactionId = credentialIssueContext.accessToken().getJWTClaimsSet().getStringClaim("tx_id");
        } catch (ParseException e) {
            throw new IssuerServerException("internal_server_error", "Missing claim in internal access token %s".formatted("tx_id"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Map<String, Object> storedClaims = cache.retrieveClaims(new IssuanceTransactionId(transactionId));
        DocumentMetadata documentMetadata = getDocumentMetadata(new CredentialMetadataContext(credentialIssueContext.credentialConfigurationId(), null, null));
        try {
            return storedClaims.keySet().stream()
                    .map(documentMetadata::findClaimMetadata)
                    .filter(Objects::nonNull)
                    .map(claimMetadata -> getClaim(claimMetadata, storedClaims)).toList();
        } catch (ClaimsSourceFormatException e) {
            throw new ClaimsSourceInvalidDataException(this.getAuthorativeSourceName(), e.getErrorDescription(), e);
        }
    }

    public Claim getClaim(ClaimMetadata claim, Map<String, Object> storedClaims) {
        return claimValueConverter.convertClaim(claim, storedClaims);
    }

}
