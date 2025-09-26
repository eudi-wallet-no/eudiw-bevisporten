package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import lombok.Getter;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * An abstract claims source retrieving claims from a OAuth2 resource server.
 */
@Getter
public abstract class OAuth2ResourceServerClaimsSource<C> implements ClaimsSource {

    private ClaimsSourceProperties properties;
    private RestClient restClient;

    public void init(ClaimsSourceProperties properties) {
        this.properties = properties;
        this.restClient = createRestClient(properties);
    }

    private RestClient createRestClient(ClaimsSourceProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient
                        .newBuilder()
                        .connectTimeout(Duration.of(properties.getConnectTimeoutMillis(), ChronoUnit.MILLIS))
                        .build());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());
        return RestClient.builder()
                .baseUrl(properties.getResourceServer())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {
        C data = (C) restClient
                .get()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.serialize())
                .retrieve()
                .body(getResponseClass());
        return mapClaims(data);
    }

    protected abstract Class<C> getResponseClass();

    protected abstract List<Claim> mapClaims(C claimsData);

}
