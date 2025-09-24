package no.idporten.eudiw.issuer.claimssource.pid;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.lib.maskinporten.client.JwtGrantTokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FregConfiguration {

    private final String fregUri;

    public FregConfiguration(@Value("${freg.uri}") String fregUri, JwtGrantTokenInterceptor jwtGrantTokenInterceptor) {
        this.fregUri = fregUri;
    }

    @Bean("fregRestClient")
    public RestClient issuerServerRestClient(JwtGrantTokenInterceptor jwtGrantTokenInterceptor) {

        return RestClient.builder()
                .baseUrl(fregUri)
                .requestInterceptor(jwtGrantTokenInterceptor)
                .requestInterceptor(
                        (request, body, execution) -> {
                            ClientHttpResponse response = execution.execute(request, body);
                            if (response.getStatusCode() == HttpStatus.REQUEST_TIMEOUT) {
                                throw new FregIOException("io_error", "Request timeout against FREG");
                            }
                            if (response.getStatusCode().is5xxServerError()) {
                                throw new IssuerServerException("server_error", "FREG returned: %s".formatted(response.getStatusText()), HttpStatus.valueOf(response.getStatusCode().value()));
                            }
                            return response;
                        }
                )
                .build();
    }

}
