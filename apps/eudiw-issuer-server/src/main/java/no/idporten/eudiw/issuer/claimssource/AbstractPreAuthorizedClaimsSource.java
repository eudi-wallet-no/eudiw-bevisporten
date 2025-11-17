package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.cache.ClaimsSourceCache;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.StringValue;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
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

    protected abstract DocumentMetadata getDocumentMetadata();


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

    public final Map<String, Object> validate(Map<String, Object> claims) {
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
        Map<String, Object> storedClaims = cache.retrieveClaims(new IssuanceTransactionId(transactionId));

        return storedClaims.keySet().stream()
                .map(claimName -> getDocumentMetadata().findClaimMetadata(claimName))
                .filter(Objects::nonNull)
                .map(claimMetadata -> getClaim(claimMetadata, storedClaims) ).toList();
    }

    public Claim getClaim(ClaimMetadata claimMetadata, Map<String, Object> storedClaims) {
        StringValue value = new StringValue((String) storedClaims.get(claimMetadata.name()));
        return Claim.builder().path(claimMetadata.name()).value(value).build();
    }

}
