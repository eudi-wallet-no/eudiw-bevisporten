package no.idporten.eudiw.issuer.ui.demo.issuer;

import no.idporten.eudiw.issuer.ui.demo.exception.IssuerServerException;
import no.idporten.eudiw.issuer.ui.demo.issuer.config.IssuerServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Service
public class IssuerServerService {

    private final IssuerServerProperties issuerServerProperties;

    private final RestClient restClient;

    private final Logger log = LoggerFactory.getLogger(IssuerServerService.class);

    @Autowired
    public IssuerServerService(@Qualifier("issuerServerRestClient") RestClient restClient, IssuerServerProperties issuerServerProperties) {
        this.issuerServerProperties = issuerServerProperties;
        this.restClient = restClient;
    }


    public String startIssuance(String json) {
        String path = issuerServerProperties.getIssuanceEndpoint();
        return callIssuerServer(path, json);
    }

    private String callIssuerServer(final String path, String json) {
        try {
            String result = restClient.post().uri(
                            path).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).body(json).retrieve()
                    .body(String.class);
            log.debug("search for " + json + ". Returned: " + result);
            return result;
        } catch (HttpClientErrorException e) {
            throw new IssuerServerException("Configuration error against issuer-server? path=" + path, e);
        } catch (HttpServerErrorException e) {
            throw new IssuerServerException("callIssuerServer failed for input" + json, e);
        }
    }

    // temp for testing before issuer server is ready
    //https://kontaktregisteret.dev/swagger-ui/index.html#/Personer/getUsersV2
    private Object findPersonInKrr(String path) {
        String synFnr = "56814900792";
        String fnr = "17912099997";
        Persons persons = new Persons(Collections.singletonList(fnr));

        Object result = restClient.post().uri(
                        path).accept(MediaType.APPLICATION_JSON).body(persons).retrieve()
                .body(Object.class);
        log.debug("search for " + fnr + ". Returned: " + result);
        return result;
    }

    public record Persons(List<String> personidentifikatorer) {
    }


}
