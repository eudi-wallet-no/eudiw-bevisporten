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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NnidClaimsSource implements PreAuthorizedClaimsSource {

    public static final String NAMESPACE = "no:skatteetaten:nnid:1";
    public static final String ATTRIBUTE_NNID = "norwegian_national_id_number";
    public static final String ATTRIBUTE_NNID_STATUS = "norwegian_national_id_number_status";
    public static final String ATTRIBUTE_NNID_TYPE = "norwegian_national_id_number_type";

    private ClaimsSourceProperties properties;

    private Map<String, List<Claim>> claimsCache = new HashMap<>();

    protected Claim findClaim(String path, List<Claim> claims) {
        return claims.stream()
                .filter(claim -> claim.getPath().getFirst().equals(path))
                .findFirst()
                .orElseThrow(() ->
                        new IssuerServerException("invalid_request", "Missing required path %s".formatted(path), HttpStatus.BAD_REQUEST));

    }

    public void validate(List<Claim> claims) {
        findClaim(ATTRIBUTE_NNID, claims);
        findClaim(ATTRIBUTE_NNID_STATUS, claims);
        findClaim(ATTRIBUTE_NNID_TYPE, claims);
    }

    @Override
    public String store(String transactionId, List<Claim> claims) {
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

    @Override
    public ClaimsSourceMetadata getMetadata() {
        return ClaimsSourceMetadata.builder()
                .display(Display.builder().name("Norwegian National identification number").build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("norwegian_national_id_number")
                        .display(Display.builder().name("ID-number").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("norwegian_national_id_number_status")
                        .display(Display.builder().name("Status").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("norwegian_national_id_number_type")
                        .display(Display.builder().name("Type").build()).build())
                .build();
    }

    @SneakyThrows
    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {
        final String transactionId = accessToken.getJWTClaimsSet().getStringClaim("tx_id");
        List<Claim> storedClaims = claimsCache.get(transactionId);
        List<Claim> claims = new ArrayList<>();
        claims.add(Claim.builder().path(NAMESPACE).path(ATTRIBUTE_NNID).value(findClaim(ATTRIBUTE_NNID, storedClaims).getValue()).build());
        claims.add(Claim.builder().path(NAMESPACE).path(ATTRIBUTE_NNID_STATUS).value(findClaim(ATTRIBUTE_NNID_STATUS, storedClaims).getValue()).build());
        claims.add(Claim.builder().path(NAMESPACE).path(ATTRIBUTE_NNID_TYPE).value(findClaim(ATTRIBUTE_NNID_TYPE, storedClaims).getValue()).build());
        return claims;
    }

}
