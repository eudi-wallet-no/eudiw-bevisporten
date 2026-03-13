package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.cache.ClaimsSourceCache;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceFormatException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.credentials.ClaimDataTypeValidator;
import no.idporten.eudiw.issuer.credentials.ClaimValueConverter;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
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
    public final CredentialData validate(ExtendedCredentialMetadata credentialMetadata, CredentialData credentialData) {
        if (credentialMetadata == null) {
            throw new IssuerServerException("invalid_request", "credentialMetadata null in request.", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> claims = credentialData.claims();
        for (ExtendedClaimsDescription extendedClaimsDescription : credentialMetadata.claims()) {
            validateClaim(extendedClaimsDescription, claims);
        }

        // reject all unknown claims
        for (String claimName : claims.keySet()) {
            if (credentialMetadata.findClaimMetadata(claimName) == null) {
                throw new IssuerServerException("invalid_request", "Unsupported claims in request.", HttpStatus.BAD_REQUEST);
            }
        }
        return new CredentialData(Collections.unmodifiableMap(claims), credentialData.credentialConfigurationId());
    }

    protected final void validateClaim(ExtendedClaimsDescription extendedClaimsDescription, Map<String, Object> claims) {
        if (extendedClaimsDescription.mandatory() && !claims.containsKey(extendedClaimsDescription.name())) {
            throw new IssuerServerException("invalid_request", "Missing required claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
        claimDataTypeValidator.validate(extendedClaimsDescription, claims.get(extendedClaimsDescription.name()));
    }

    @Override
    public IssuanceTransactionId store(PreAuthorizedIssuanceContext issuanceContext, final Map<String, Object> claims, Duration lifetime) {
        cache.storeClaims(issuanceContext.credentialIssuerTenant(), issuanceContext.issuanceTransactionId(), claims, lifetime);
        return issuanceContext.issuanceTransactionId();
    }

    @Override
    public final List<Claim> issueClaims(CredentialIssueContext credentialIssueContext) {
        final String transactionId;
        try {
            transactionId = credentialIssueContext.accessToken().getJWTClaimsSet().getStringClaim("tx_id");
        } catch (ParseException e) {
            throw new IssuerServerException("internal_server_error", "Missing claim in internal access token %s".formatted("tx_id"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Map<String, Object> storedClaims = cache.retrieveClaims(credentialIssueContext.credentialIssuerTenant(), new IssuanceTransactionId(transactionId));
        ExtendedCredentialMetadata extendedCredentialMetadata = credentialIssueContext.credentialMetadata();
        try {
            return storedClaims.keySet().stream()
                    .map(extendedCredentialMetadata::findClaimMetadata)
                    .filter(Objects::nonNull)
                    .map(claimMetadata -> getClaim(claimMetadata, storedClaims)).toList();
        } catch (ClaimsSourceFormatException e) {
            throw new ClaimsSourceInvalidDataException(this.getAuthorativeSourceName(), e.getErrorDescription(), e);
        }
    }

    public Claim getClaim(ExtendedClaimsDescription claim, Map<String, Object> storedClaims) {
        return claimValueConverter.convertClaim(claim, storedClaims);
    }

}
