package no.idporten.eudiw.issuer.authoritativesources.pid;

import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceIOException;
import no.idporten.lib.maskinporten.client.JwtGrantTokenInterceptor;
import no.idporten.lib.maskinporten.client.MaskinportenClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.Charset;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.FREG;

@Service
public class FregConfiguration {

    private final FregProperties fregProperties;

    public FregConfiguration(FregProperties fregProperties) {
        this.fregProperties = fregProperties;
    }

    @Bean
    public JwtGrantTokenInterceptor jwtGrantTokenInterceptor(MaskinportenClients maskinportenClients) {
        return new JwtGrantTokenInterceptor(maskinportenClients.getClient("freg"));
    }

    @Bean("fregRestClient")
    public RestClient issuerServerRestClient(JwtGrantTokenInterceptor jwtGrantTokenInterceptor) {
        return RestClient.builder()
                .baseUrl(fregProperties.uri())
                .requestFactory(getClientHttpRequestFactory())
                .requestInterceptor(jwtGrantTokenInterceptor)
                .requestInterceptor(
                        (request, body, execution) -> {
                            ClientHttpResponse response = execution.execute(request, body);
                            if (response.getStatusCode() == HttpStatus.REQUEST_TIMEOUT) {
                                throw new ClaimsSourceIOException(FREG.name(), "Request timeout against FREG");
                            }
                            if (response.getStatusCode().is5xxServerError()) {
                                handleErrorResponseAs500(response);
                            }
                            return response;
                        }
                )
                .build();
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(fregProperties.readTimeout());
        factory.setConnectTimeout(fregProperties.connectTimeout());
        return factory;
    }

    static void handleErrorResponseAs500(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data fra authoritative source.  Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new ClaimsSourceException(FREG.name(), "server_error", "Failed to get information from Folkeregisteret", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }


}
