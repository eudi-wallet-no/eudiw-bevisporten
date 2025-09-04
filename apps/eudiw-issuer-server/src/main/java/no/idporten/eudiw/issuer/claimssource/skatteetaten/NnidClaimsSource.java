package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import com.nimbusds.jwt.JWT;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.Claim;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NnidClaimsSource implements PreAuthorizedClaimsSource {

    public static final String ATTRIBUTE_NNID = "norwegian_national_id_number";
    public static final String ATTRIBUTE_NNID_STATUS = "norwegian_national_id_number_status";
    public static final String ATTRIBUTE_NNID_TYPE = "norwegian_national_id_number_type";

    private ClaimsSourceProperties properties;

    private Map<String, Map<String, String>> claimsCache = new HashMap<>();

    protected void validateClaim(String name, Map<String, String> claims) {
        if (! claims.containsKey(name)) {
            throw new IssuerServerException("invalid_request", "Missing required claim %s".formatted(name), HttpStatus.BAD_REQUEST);
        }
        if (! StringUtils.hasLength(claims.get(name))) {
            throw new IssuerServerException("invalid_request", "Missing required value for claim %s".formatted(name), HttpStatus.BAD_REQUEST);
        }
    }

    public void validate(Map<String, String> claims) {
        validateClaim(ATTRIBUTE_NNID, claims);
        validateClaim(ATTRIBUTE_NNID_STATUS, claims);
        validateClaim(ATTRIBUTE_NNID_TYPE, claims);
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
        return ClaimsSourceMetadata.builder()
                .display(Display.builder().name("Norwegian National identification number").build())
                .claim(ClaimsDescription.builder()
                        .path(namespace()).path("norwegian_national_id_number")
                        .display(Display.builder().name("ID-number").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(namespace()).path("norwegian_national_id_number_status")
                        .display(Display.builder().name("Status").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(namespace()).path("norwegian_national_id_number_type")
                        .display(Display.builder().name("Type").build()).build())
                .build();
    }

    @SneakyThrows
    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {
        final String transactionId = accessToken.getJWTClaimsSet().getStringClaim("tx_id");
        Map<String, String> storedClaims = claimsCache.get(transactionId);
        List<Claim> claims = new ArrayList<>();
        claims.add(Claim.builder().path(namespace()).path(ATTRIBUTE_NNID).value(storedClaims.get(ATTRIBUTE_NNID)).build());
        claims.add(Claim.builder().path(namespace()).path(ATTRIBUTE_NNID_STATUS).value(storedClaims.get(ATTRIBUTE_NNID_STATUS)).build());
        claims.add(Claim.builder().path(namespace()).path(ATTRIBUTE_NNID_TYPE).value(storedClaims.get(ATTRIBUTE_NNID_TYPE)).build());
        return claims;
    }

}
