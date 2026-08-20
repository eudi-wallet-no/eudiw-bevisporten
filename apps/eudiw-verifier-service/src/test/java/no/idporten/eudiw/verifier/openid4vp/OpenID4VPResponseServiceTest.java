package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.testdata.TrustlistTestdata;
import no.idporten.eudiw.verifier.trustlist.TrustlistsProperties;
import no.idporten.eudiw.verifier.trustlist.TrustlistService;
import no.idporten.eudiw.verifier.statuslist.TokenStatuslistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;


@ActiveProfiles("junit")
@SpringBootTest
class OpenID4VPResponseServiceTest {

    public static URI XMLTRUSTLISTURL =URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_tsl.xtsl");
    public static URI JSONTRUSTLISTURL = URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_pid.jws");


    @Autowired
    private TrustlistsProperties trustlistConfig;

    private MockRestServiceServer mockServer;

    @MockitoSpyBean
    private TrustlistService trustlistService;

    @MockitoSpyBean
    TokenStatuslistService tokenStatuslistService;

    @Autowired
    JsonMapper jsonMapper;

    @Mock
    private VerificationTransactionService verificationService;

    @Autowired
    private OpenID4VPResponseService openID4VPResponseService;

    private TrustlistTestdata trustlistTestdata;


    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient trustlistRestClient = builder.build();
        openID4VPResponseService = new OpenID4VPResponseService(verificationService,tokenStatuslistService, jsonMapper, trustlistRestClient, trustlistConfig);
        trustlistService = new TrustlistService(trustlistRestClient, trustlistConfig);
        this.trustlistTestdata = new TrustlistTestdata();
    }

    @Test
    void pidTrustlist() throws Exception {
        String vpToken = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCIsIng1YyI6WyJNSUlEQ2pDQ0FyR2dBd0lCQWdJSUJQZ01Eb3YwTWhFd0NnWUlLb1pJemowRUF3SXdnWWN4SGpBY0JnTlZCR0VURlU1VVVrNVBMVTVQUms5U0xqazVNVGd5TlRneU56RUxNQWtHQTFVRUJoTUNUazh4SkRBaUJnTlZCQW9URzBSSlIwbFVRVXhKVTBWU1NVNUhVMFJKVWtWTFZFOVNRVlJGVkRFeU1EQUdBMVVFQXhNcFpXbGtZWE15YzJGdVpHdGhjM05sSUZCSlJDQlFjbTkyYVdSbGNpQkRRU0F5SUhONWMzUmxjM1F3SGhjTk1qWXdOREk0TVRBME1URXdXaGNOTWpjd05ESTRNVEEwTVRFd1dqQnFNUXN3Q1FZRFZRUUdFd0pPVHpFa01DSUdBMVVFQ2d3YlJFbEhTVlJCVEVsVFJWSkpUa2RUUkVsU1JVdFVUMUpCVkVWVU1SVXdFd1lEVlFRRERBeFFTVVF0ZFhSemRHVmtaWEl4SGpBY0JnTlZCR0VNRlU1VVVrNVBMVTVQUms5U0xqazVNVGd5TlRneU56QlpNQk1HQnlxR1NNNDlBZ0VHQ0NxR1NNNDlBd0VIQTBJQUJBVlhtaTErbU9kWVdIcjdZWURJcC9UTE0yYytoUGhDWjdZcTdodUF1TkExYlZlNWxJWUxrVHdlNi9TeXorWjVmNktFdFQ5N1dxQlAxQkU5WmtKWGZLU2pnZ0VoTUlJQkhUQWZCZ05WSFNNRUdEQVdnQlM2OEJmeEd6UTIxZStyT3lWaGk2aEt0Z3ZxU3pBZEJnTlZIUTRFRmdRVWxzZVRvc3hCNURkbTNjUFhXckxoQkpQMWVIRXdEQVlEVlIwVEFRSC9CQUl3QURCWUJnTlZIUjhFVVRCUE1FMmdTNkJKaGtkb2RIUndjem92TDJOaExtVnBaR0Z6TW5OaGJtUnJZWE56WlM1a1pYWXZkakV2WTJWeWRITXZhVzUwWlhKdFpXUnBZWFJsY3k5d2FXUmZjSEp2ZG1sa1pYSXlMbU55YkRCakJnZ3JCZ0VGQlFjQkFRUlhNRlV3VXdZSUt3WUJCUVVITUFLR1IyaDBkSEJ6T2k4dlkyRXVaV2xrWVhNeWMyRnVaR3RoYzNObExtUmxkaTkyTVM5alpYSjBjeTlwYm5SbGNtMWxaR2xoZEdWekwzQnBaRjl3Y205MmFXUmxjakl1WTJWeU1BNEdBMVVkRHdFQi93UUVBd0lGb0RBS0JnZ3Foa2pPUFFRREFnTkhBREJFQWlCRE9WdmdMMWpZUHd1MHNHTWo2aDNjK0pkei81T000SHFIV2dMalJ6bEY1UUlnRXhhdTUxekVXSFlPTm9xQ3BsZ0VocG43ckhHOFBpbjRVMTRnck50U0taUT0iXX0.eyJfc2QiOlsib1FnLUVuc0FkaXdfMjR0OXhyZkF5eEpnSkVmWDk2QjJiMlJlQTdza2dCYyIsIkwzbVRZdGkxZE9IOFVPME8xUGZDODZjalYyU3ZsN0RVaklRbkR6aWJvaTAiLCJZWXhsaFdpNnhjdTF4d3RfSnlrNFMtODVzSF9hbTJCZjZCdktyWUFoRVhVIiwiR3FQMDBFcU5mN19kbGhnTUczUTZwZTZFQ0J3VDRKWURBZUJ0M1ZXb21uSSIsIjZCQ0RpOGttZmZkOWEtVWFCQnM3MHFDb09jeG1vOGJteGJNTWZjZVkwb2ciLCJmYVV0RXJ3cG5vZ0paNXZsYjc3bHBpVThrSkpJM1RwalJTLUpoVzhucjdrIiwiUWllSElGYWd2cF91aXZLX2h4ZGVrZGt5MnhnZHlzYTYxRGVfRTVyNGx5VSIsIk1EcTdCUDc1Ym9ydDAzRHNqckhlaGFXejdHaGhkd0kzR2pPakpjRUhOZXciLCI5OWNyNGdhejlyeFVNdG1MZTVNRkloM2p5ZVU4LXRYdVo3Q1pMNjd1TVZVIl0sInZjdCI6InVybjpldWRpOnBpZDoxIiwiX3NkX2FsZyI6InNoYS0yNTYiLCJpc3MiOiJodHRwczovL3V0c3RlZGVyLmVpZGFzMnNhbmRrYXNzZS5kZXYvcGlkIiwiY25mIjp7Imp3ayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6IlNJbkFsNHRpb0V5X3ROeEd0aUlqZnNtS0wxdkNUcDZSUlNCbDB6eG1qR0UiLCJ5IjoibUV1a3lUdjdNbC1WNjJpd05CakhVRVRqMXJPLXpVQ0VSeFd5UUdOWWNDWSJ9fSwiZXhwIjoxODE2MTY2ODEwLCJpYXQiOjE3ODQ2MzA4MTAsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjQyMDg0NiwidXJpIjoiaHR0cHM6Ly9zdGF0dXMuZWlkYXMyc2FuZGthc3NlLmRldi9saXN0cy8xIn19fQ.aFJQ9k4Xkso-88mb-wabOT6NvdCCuaWLSRLVv4dHYZwdjHRMWcXjARt6FIRdLtPd4glRhYv3YinZg1zbE9RHfg~WyJTa0NHdUMyUVBBb2J2bkh3WHFVdm5BIiwiZ2l2ZW5fbmFtZSIsIkZBTlRBU0lGVUxMIl0~WyJuanFGLTVWcjhHUzlCOUR1cVdKdWJnIiwiYmlydGhkYXRlIiwiMTk2My0wNS0xNiJd~WyJIT3BxbEs2dXoxZVdETC1vU01KS01BIiwibmF0aW9uYWxpdGllcyIsWyJOTyJdXQ~WyJfZVhGaWo4ZkY2dHpSd3B2ZGJINGxBIiwiZmFtaWx5X25hbWUiLCJIWUxMRSJd~WyJNaWNnQVFFNUFsRXphZ2tkdV9MQ2R3IiwicGxhY2Vfb2ZfYmlydGgiLHsiY291bnRyeSI6Ik5PIn1d~eyJ0eXAiOiJrYitqd3QiLCJhbGciOiJFUzI1NiJ9.eyJzZF9oYXNoIjoiMjNFUzV1X2EyMkpia05tQVdTZU9ya3hId051T3IwREp6TTZoUkwwdFhtWSIsImF1ZCI6Ing1MDlfaGFzaDpISk5RMktUX201N0ZjOVhUbENYeFBwWTF2Z2laNWxaNXVTYzNiZHc1RGJJIiwibm9uY2UiOiI1MTM2YjM3MS1mMzdiLTQ1NjQtOGIyZS01YzI0ZDlmZDQ4NWEiLCJpYXQiOjE3ODcyMTgyOTN9.9wlV8onuLrxwBA27WZSttYKXqSzfE9SS_nWgCGmcBMvybElL97AaZzCk-u2t2X9ZrsRlazmOugizcnH5KRfRlw";

        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getXmlTrustlist(), MediaType.parseMediaType("application/vnd.etsi.tsl+xml")));
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getXmlTrustlist(), MediaType.parseMediaType("application/vnd.etsi.tsl+xml")));
        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getJsonTrustlist(), MediaType.parseMediaType("application/jose+json")));
        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getJsonTrustlist(), MediaType.parseMediaType("application/jose+json")));

        Mockito.when(tokenStatuslistService.checkStatus(URI.create(anyString()), anyInt(), anyString(), Instant.ofEpochMilli(anyLong()))).thenReturn(ValidationStatus.VALID);

        VerifiedCredential verifiedCredential = openID4VPResponseService.retrieveClaimsFromSDJwtCredential(vpToken, false);

        assertAll(
                () -> assertEquals("FANTASIFULL", verifiedCredential.claims().get("given_name"))
        );

    }
}
