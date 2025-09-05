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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NnidClaimsSource implements PreAuthorizedClaimsSource {

    public static final String ATTRIBUTE_NNID = "norwegian_national_id_number";
    public static final String ATTRIBUTE_NNID_STATUS = "norwegian_national_id_number_status";
    public static final String ATTRIBUTE_NNID_TYPE = "norwegian_national_id_number_type";
    private static final Map<String, String> REQUIRED_CLAIMS = new HashMap<>();

    private ClaimsSourceProperties properties;

    private final Map<String, Map<String, String>> claimsCache = new HashMap<>();

    protected void validateClaim(String name, Map<String, String> claims) {
        if (!claims.containsKey(name)) {
            throw new IssuerServerException("invalid_request", "Missing required claim %s".formatted(name), HttpStatus.BAD_REQUEST);
        }
        String value = claims.get(name);
        if (!StringUtils.hasLength(value)) {
            throw new IssuerServerException("invalid_request", "Missing required value for claim %s".formatted(name), HttpStatus.BAD_REQUEST);
        }

        Pattern pattern = Pattern.compile(REQUIRED_CLAIMS.get(name), Pattern.CASE_INSENSITIVE);
        Matcher match = pattern.matcher(value);
        if (!match.matches() ) {
            throw new IssuerServerException("invalid_request", "Invalid format for required value for claim %s".formatted(name), HttpStatus.BAD_REQUEST);
        }
    }

    public void validate(Map<String, String> claims) {
        if (claims.size() != REQUIRED_CLAIMS.size()) {
            throw new IssuerServerException("invalid_request", "Wrong number of claims provided, must be exactly %s".formatted(REQUIRED_CLAIMS.size()), HttpStatus.BAD_REQUEST);
        }
        for (String requiredClaim : REQUIRED_CLAIMS.keySet()) {
            validateClaim(requiredClaim, claims);
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
        REQUIRED_CLAIMS.put(ATTRIBUTE_NNID, "\\d{11}");
        REQUIRED_CLAIMS.put(ATTRIBUTE_NNID_STATUS, "kontrollert|unik|ikke kontrollert");
        REQUIRED_CLAIMS.put(ATTRIBUTE_NNID_TYPE, "d-nummer|f-nummer");
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
        return ClaimsSourceMetadata.builder()
                .display(Display.builder().name("Norwegian National identification number").build())
                .claim(ClaimsDescription.builder()
                        .path(namespace()).path(ATTRIBUTE_NNID)
                        .display(Display.builder().name("ID-number").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(namespace()).path(ATTRIBUTE_NNID_STATUS)
                        .display(Display.builder().name("Status").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(namespace()).path(ATTRIBUTE_NNID_TYPE)
                        .display(Display.builder().name("Type").build()).build())
                .build();
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

        return REQUIRED_CLAIMS.keySet().stream().map(claimName ->
                Claim.builder().path(namespace()).path(claimName).value(storedClaims.get(claimName)).build()
        ).toList();
    }

}
