package no.idporten.eudiw.issuer.claimssource.pid;

import no.digdir.freg.FregClientInterface;
import no.digdir.freg.domain.json.*;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
public class FregIntegration implements FregClientInterface {

    private final RestClient restClient;
    private static final String PERSON_PATH = "v1/personer/{fnr}";

    public FregIntegration(@Qualifier("fregRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Folkeregisterperson getFolkeregisterPerson(String fnr, List<String> part) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(PERSON_PATH);

        for (String p : part) {
            uriBuilder.queryParam("part", p);
        }

        URI uri = uriBuilder.buildAndExpand(fnr).toUri();

        Folkeregisterperson person = restClient.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new UserNotFoundException("User not found in FREG");
                })
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new IssuerServerException("client_error", response.getStatusText(), HttpStatus.valueOf(response.getStatusCode().value()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new IssuerServerException("server_error", response.getStatusText(), HttpStatus.valueOf(response.getStatusCode().value()));
                })
                .body(Folkeregisterperson.class);

        // TODO handle IOExceptions specifically to allow metrics

        return person;
    }

    @Override
    public Folkeregisterperson getFullFolkeregisterPerson(String s, List<String> list) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public FolkeregisterOppslag getFolkeregisterOppslag(FolkeregisterOppslagRequest folkeregisterOppslagRequest, List<String> list) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String getIdentitetsgrunnlagAsString(String s, List<String> list) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Folkeregisterperson getEntydigSoekMotFolkeregisterPerson(Boolean aBoolean, String s, String s1, String s2, String s3, List<String> list) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public FolkeregisterHendelseHolder[] getHendelser(Integer integer) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public FREGHendelsesdokument getHendelse(String s) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public OrderBulkBatchResponse createBatchJob(OrderBulkBatchRequest orderBulkBatchRequest) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PidListResponse getBatchPage(String s, int i) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
