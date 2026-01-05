package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.cache.ClaimsSourceCache;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceFormatException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract non-sealed class AbstractPreAuthorizedClaimsSource implements PreAuthorizedClaimsSource {

    private ClaimsSourceProperties properties;

    private ClaimsSourceCache cache;

    private final ClaimValueConverter claimValueConverter = new ClaimValueConverter();

    @Autowired
    public void setClaimsSourceCache(ClaimsSourceCache claimsSourceCache) {
        this.cache = claimsSourceCache;
    }

    public final void validateClaim(ClaimMetadata claimMetadata, Map<String, Object> claims) {
        if (claimMetadata.mandatory() && !claims.containsKey(claimMetadata.name())) {
            throw new IssuerServerException("invalid_request", "Missing required claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        if (ClaimMetadata.TYPE_STRING.equals(claimMetadata.type())) {
            validateStringValue(claimMetadata, claims);
        } else if (ClaimMetadata.TYPE_FULLDATE.equals(claimMetadata.type())) {
            final LocalDate value = (LocalDate) claims.get(claimMetadata.name());
            if (claimMetadata.mandatory() && value == null) {
                throw new IssuerServerException("invalid_request", "Missing required value for fulldate claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
            }
        } else if (ClaimMetadata.TYPE_MAP.equals(claimMetadata.type())) {
            final Map<String, Object> value = (Map<String, Object>) claims.get(claimMetadata.name());
            if (claimMetadata.mandatory() && value.isEmpty()) {
                throw new IssuerServerException("invalid_request", "Missing required Map value for map claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
            }
        }
        // TODO more validation
    }

    private static void validateStringValue(ClaimMetadata claimMetadata, Map<String, Object> claims) {
        final String value = (String) claims.get(claimMetadata.name());
        if (claimMetadata.mandatory() && !StringUtils.hasLength(value)) {
            throw new IssuerServerException("invalid_request", "Missing required value for string claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        if (!claimMetadata.mandatory() && !StringUtils.hasLength(value)) {
            return;
        }
        if (!value.matches(claimMetadata.validationRegex())) {
            throw new IssuerServerException("invalid_request", "Invalid format for required value for string claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
    }


    @Override
    public final Map<String, Object> validate(DocumentMetadata credentialMetadata, Map<String, Object> claims) {
        for (ClaimMetadata claimMetadata : credentialMetadata.claims()) {
            validateClaim(claimMetadata, claims);
        }
        // reject all unknown claims
        for (String claimName : claims.keySet()) {
            if (getDocumentMetadata(null).findClaimMetadata(claimName) == null) {
                throw new IssuerServerException("invalid_request", "Unsupported claims in request.", HttpStatus.BAD_REQUEST);
            }
        }
        return Collections.unmodifiableMap(claims);
    }

    @Override
    public IssuanceTransactionId store(IssuanceTransactionId issuanceTransactionId, final Map<String, Object> claims, Duration lifetime) {
        cache.storeClaims(issuanceTransactionId, claims, lifetime);
        return issuanceTransactionId;
    }

    @Override
    public void init(ClaimsSourceProperties properties) {
        this.properties = properties;
        if (this.cache == null) {
            throw new IllegalStateException("Claims cache not initialized for claims source supporting credential types %s".formatted(properties.getCredentialTypes()));
        }
    }

    @Override
    public final ClaimsSourceProperties getProperties() {
        return properties;
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
