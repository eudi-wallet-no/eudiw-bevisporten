package no.idporten.eudiw.connector.authoritativesources.freg;

import no.digdir.freg.audit.AuditLog;
import no.digdir.freg.eventlog.EventLog;
import no.digdir.freg.service.FregResultMapper;
import no.digdir.logging.event.EventLogger;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceIOException;
import no.idporten.lib.maskinporten.client.JwtGrantTokenInterceptor;
import no.idporten.lib.maskinporten.client.MaskinportenClients;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.Charset;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.FREG;


@Configuration
@EnableConfigurationProperties(FregProperties.class)
public class FregConfiguration {

    private final FregProperties fregProperties;
    private final AuditLogger auditLogger;
    private final EventLogger eventLogger;
    private final JsonMapper jsonMapper;


    public FregConfiguration(FregProperties fregProperties, AuditLogger auditLogger, EventLogger eventLogger, JsonMapper jsonMapper) {
        this.fregProperties = fregProperties;
        this.auditLogger = auditLogger;
        this.eventLogger = eventLogger;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public JwtGrantTokenInterceptor jwtGrantTokenInterceptor(MaskinportenClients maskinportenClients) {
        return new JwtGrantTokenInterceptor(maskinportenClients.getClient(fregProperties.maskinportenClient()));
    }

    @Bean
    public no.digdir.freg.service.FregService fregService(FregIntegration fregIntegration) {
        return new no.digdir.freg.service.FregService(
                new FregResultMapper(),
                new AuditLog(auditLogger),
                new EventLog(eventLogger),
                jsonMapper,
                fregIntegration);
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
                                throw new ClaimsSourceIOException(FREG, "Request timeout against FREG");
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
        throw new AuthoritativeSourceException(FREG, "server_error", "Failed to get information from Folkeregisteret", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }


}
