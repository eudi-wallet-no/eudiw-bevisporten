package no.idporten.eudiw.issuer.claimssource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.openid4vci.protocol.Subject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.BYOB;

/**
 * Generic claims source for pre-authorized issuance where the credential data is pulled from authoritative source
 * standard API.
 */
@Service
public class HttpPullPreAuthorizedClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final RestClient restClient;

    public HttpPullPreAuthorizedClaimsSource() {
        this.restClient = fooRestClient();
    }

    public RestClient fooRestClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        // TODO inject og hvor skal config komme fra
//        clientHttpRequestFactory.setConnectTimeout(xxx.connectTimeout());
//        clientHttpRequestFactory.setReadTimeout(xxx.readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @SneakyThrows // TODO Bare temp for uttesting denne koden her
    @Override
    public CredentialData pull(PreAuthorizedIssuanceContext preAuthorizedIssuanceContext) {
        Subject subject = new Subject(preAuthorizedIssuanceContext.accessToken().getJWTClaimsSet().getStringClaim("pid"));
        Request request = new Request(subject);
        Response response = restClient.post()
                .uri(preAuthorizedIssuanceContext.credentialConfiguration().getCredentialIssuerContext().getCredentialDataSourceUri())
                .body(request)
                .retrieve()
                .body(Response.class);
        return new CredentialData(response.credentialData());
    }

    @Override
    public String getAuthorativeSourceName() {
        // TODO fjerne dette fra utsteder og exceptions?
        return BYOB.name();
    }

    record Request(@JsonProperty("subject") Subject subject){}

    record Response(@JsonProperty("credential_data") Map<String, Object> credentialData){}

}
