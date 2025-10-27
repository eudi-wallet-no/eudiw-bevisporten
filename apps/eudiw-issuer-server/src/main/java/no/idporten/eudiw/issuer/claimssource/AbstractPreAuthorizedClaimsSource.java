package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.cache.ClaimsSourceCache;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.StringValue;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.time.Duration;
import java.util.*;

public abstract non-sealed class AbstractPreAuthorizedClaimsSource implements PreAuthorizedClaimsSource {

    private ClaimsSourceProperties properties;

    private ClaimsSourceCache cache;

    protected abstract DocumentMetadata getDocumentMetadata();


    @Autowired
    public void setClaimsSourceCache(ClaimsSourceCache claimsSourceCache) {
        this.cache = claimsSourceCache;
    }

    public final void validateClaim(ClaimMetadata claimMetadata, Map<String, String> claims) {
        if (claimMetadata.mandatory() && !claims.containsKey(claimMetadata.name())) {
            throw new IssuerServerException("invalid_request", "Missing required claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        final String value = claims.get(claimMetadata.name());
        if (claimMetadata.mandatory() && !StringUtils.hasLength(value)) {
            throw new IssuerServerException("invalid_request", "Missing required value for claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        if (!claimMetadata.mandatory() && !StringUtils.hasLength(value)) {
            return;
        }
        if (!value.matches(claimMetadata.validationRegex())) {
            throw new IssuerServerException("invalid_request", "Invalid format for required value for claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
    }

    public final Map<String, String> validate(Map<String, String> claims) {
        // validate all known claims
        for (ClaimMetadata claimMetadata : getDocumentMetadata().claims()) {
            validateClaim(claimMetadata, claims);
        }
        // reject all unknown claims
        for (String claimName : claims.keySet()) {
            if (getDocumentMetadata().findClaimMetadata(claimName) == null) {
                throw new IssuerServerException("invalid_request", "Unsupported claims in request.", HttpStatus.BAD_REQUEST);
            }
        }
        return Collections.unmodifiableMap(claims);
    }

    @Override
    public final IssuanceTransactionId store(IssuanceTransactionId issuanceTransactionId, Map<String, String> claims, Duration lifetime) {
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
    public final ClaimsSourceMetadata getMetadata() {
        ClaimsSourceMetadata.ClaimsSourceMetadataBuilder builder = ClaimsSourceMetadata.builder();
        builder.displays(getDocumentMetadata().displayNames()
                .entrySet()
                .stream()
                .map(displayName -> Display.builder().locale(displayName.getKey()).name(displayName.getValue()).build()).toList());
        for (ClaimMetadata claimMetadata : getDocumentMetadata().claims()) {
            builder.claim(ClaimsDescription.builder()
                    .path(claimMetadata.name())
                    .mandatory(claimMetadata.mandatory())
                    .displays(claimMetadata.displayNames()
                            .entrySet()
                            .stream()
                            .map(displayName -> Display.builder().locale(displayName.getKey()).name(displayName.getValue()).build()).toList())
                    .build());
        }
        return builder.build();
    }

    @Override
    public final List<Claim> retrieveClaims(JWT accessToken) {
        final String transactionId;
        try {
            transactionId = accessToken.getJWTClaimsSet().getStringClaim("tx_id");
        } catch (ParseException e) {
            throw new IssuerServerException("internal_server_error", "Missing claim in internal access token %s".formatted("tx_id"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Map<String, String> storedClaims = cache.retrieveClaims(new IssuanceTransactionId(transactionId));
        return storedClaims.keySet().stream().map(claimName ->
                Claim.builder().path(claimName).value(new StringValue(storedClaims.get(claimName))).build()
        ).toList();
    }

}
