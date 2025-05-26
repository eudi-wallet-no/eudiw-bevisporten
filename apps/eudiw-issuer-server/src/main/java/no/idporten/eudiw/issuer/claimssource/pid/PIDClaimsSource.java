package no.idporten.eudiw.issuer.claimssource.pid;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.issuer.claimssource.Claim;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.OAuth2ResourceServerClaimsSource;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PIDClaimsSource extends OAuth2ResourceServerClaimsSource<Map<String, Object>> {

    public static final String NAMESPACE = "eu.europa.ec.eudi.pid.1";

    @Override
    public ClaimsSourceMetadata getMetadata() {
        return ClaimsSourceMetadata.builder()
                .display(Display.builder().name("Norwegian PID").build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("personal_administrative_number")
                        .display(Display.builder().name("Fnr/dnr").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("given_name")
                        .display(Display.builder().name("Given name").build()).build())
                .build();
    }

    @Override
    protected List<Claim> mapClaims(Map<String, Object> userInfoMap) {
        UserInfo userInfo = new UserInfo(new JSONObject(userInfoMap));
        List<Claim> claims = new ArrayList<>();
        claims.add(Claim.builder().path(NAMESPACE).path("personal_administrative_number").value(userInfo.getStringClaim("pid")).build());
        claims.add(Claim.builder().path(NAMESPACE).path("given_name").value(userInfo.getStringClaim("given_name")).build());
        return claims;
    }

    protected Class getResponseClass() {
        return new HashMap<String, Object>().getClass();
    }

}
