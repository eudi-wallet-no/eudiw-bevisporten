package no.idporten.eudiw.issuer.authoritativesources.pid;

import no.digdir.freg.FregClientInterface;
import no.digdir.freg.domain.json.*;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceIOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.FREG;
import static no.idporten.eudiw.issuer.authoritativesources.pid.FregConfiguration.handleErrorResponseAs500;

@Service
public class FregIntegration implements FregClientInterface {

    private final RestClient restClient;
    protected static final String PERSON_PATH = "v1/personer/{fnr}";

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

        try {
            return restClient.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ClaimsSourceDataNotFoundException(FREG.name(), "User not found in FREG");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponseAs500(response))
                    .body(Folkeregisterperson.class);
        }catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(FREG.name(), "IO error when calling FREG getPerson", e);
        }

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
