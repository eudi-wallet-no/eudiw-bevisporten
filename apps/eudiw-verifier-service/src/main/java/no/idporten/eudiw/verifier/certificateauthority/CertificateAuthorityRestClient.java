package no.idporten.eudiw.verifier.certificateauthority;


import no.idporten.eudiw.verifier.statuslist.TokenStatuslistConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CertificateAuthorityRestClient {

    private final CertificateAuthorityConfig certificateAuthorityConfig;
    protected static final String CA_MEDIA_TYPE = "application/x-pem-file";

    public CertificateAuthorityRestClient(CertificateAuthorityConfig certificateAuthorityConfig) {
        this.certificateAuthorityConfig = certificateAuthorityConfig;
    }

    @Bean("certificateAuthority")
    public RestClient restClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(certificateAuthorityConfig.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(certificateAuthorityConfig.readTimeout());

        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, CA_MEDIA_TYPE)
                .baseUrl(certificateAuthorityConfig.certificateAuthorityUrl().toString())
      //          .defaultHeader("api-key", certificateAuthorityConfig.apiKey())
                .build();
    }
}