package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.List;

public class OAuth2ResourceServerClaimsSource extends ClaimsSource {

    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {
        // TODO config
        String url = "https://idporten.dev/userinfo";
        RestClient restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.serialize())
                .build();
        // TODO mapping
        String data = restClient.get().retrieve().body(String.class);
        return List.of(Claim.builder().path("foo").value(data).build());
    }

}
