package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPreAuthorizedClaimsSource implements PreAuthorizedClaimsSource {

    private ClaimsSourceProperties properties;

    private final Map<String, Map<String, String>> claimsCache = new HashMap<>();

    protected abstract DocumentMetadata getDocumentMetadata();

    public final void validateClaim(ClaimMetadata claimMetadata, Map<String, String> claims) {
        if (claimMetadata.mandatory() && !claims.containsKey(claimMetadata.name())) {
            throw new IssuerServerException("invalid_request", "Missing required claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        final String value = claims.get(claimMetadata.name());
        if (claimMetadata.mandatory() && !StringUtils.hasLength(value)) {
            throw new IssuerServerException("invalid_request", "Missing required value for claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
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
    public final String store(String transactionId, Map<String, String> claims) {
        claimsCache.put(transactionId, claims);
        return transactionId;
    }

    @Override
    public void init(ClaimsSourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public final ClaimsSourceProperties getProperties() {
        return properties;
    }

    private String namespace() {
        return getProperties().getDoctype();
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
                    .path(namespace()).path(claimMetadata.name())
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
        Map<String, String> storedClaims = claimsCache.get(transactionId);
        return storedClaims.keySet().stream().map(claimName ->
                Claim.builder().path(namespace()).path(claimName).value(storedClaims.get(claimName)).build()
        ).toList();
    }

}
