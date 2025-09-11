package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.Claim;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NnidClaimsSource implements PreAuthorizedClaimsSource {

    private ClaimsSourceProperties properties;

    private final Map<String, Map<String, String>> claimsCache = new HashMap<>();

    private final DocumentMetadata documentMetadata = new DocumentMetadata(
            Map.of(
                    "no", "Norsk identitetsnummer",
                    "en", "Norwegian identification number"
            ),
            List.of(
                    new ClaimMetadata("norwegian_national_id_number",
                            Map.of(
                                    "no", "Norsk identitetsnummer",
                                    "en", "Norwegian identification number"),
                            true,
                            "^\\d{11}$"),
                    new ClaimMetadata("norwegian_national_id_number_type",
                            Map.of(
                                    "no", "Type norsk identitetsnummer",
                                    "en", "Type of Norwegian identification number"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$")
            )
    );

    protected void validateClaim(ClaimMetadata claimMetadata, Map<String, String> claims) {
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

    public void validate(Map<String, String> claims) {
        // validate all known claims
        for (ClaimMetadata claimMetadata : documentMetadata.claims()) {
            validateClaim(claimMetadata, claims);
        }
        // reject all unknown claims
        for (String claimName : claims.keySet()) {
            if (documentMetadata.findClaimMetadata(claimName) == null) {
                throw new IssuerServerException("invalid_request", "Unsupported claims in request.", HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Override
    public String store(String transactionId, Map<String, String> claims) {
        validate(claims);
        claimsCache.put(transactionId, claims);
        return transactionId;
    }


    @Override
    public void init(ClaimsSourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ClaimsSourceProperties getProperties() {
        return properties;
    }

    private String namespace() {
        return getProperties().getDoctype();
    }

    @Override
    public ClaimsSourceMetadata getMetadata() {
        ClaimsSourceMetadata.ClaimsSourceMetadataBuilder builder = ClaimsSourceMetadata.builder();
        builder.displays(documentMetadata.displayNames()
                .entrySet()
                .stream()
                .map(displayName -> Display.builder().locale(displayName.getKey()).name(displayName.getValue()).build()).toList());
        for (ClaimMetadata claimMetadata : documentMetadata.claims()) {
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
    public List<Claim> retrieveClaims(JWT accessToken) {
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

    public DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

}
