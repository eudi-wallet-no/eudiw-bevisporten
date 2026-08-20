package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.trustlist.TrustlistsProperties;
import no.idporten.eudiw.verifier.trustlist.TrustlistService;
import no.idporten.eudiw.verifier.statuslist.TokenStatuslistService;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;


@ActiveProfiles("junit")
@SpringBootTest
class OpenID4VPResponseServiceTest {


    @Autowired
    private TrustlistsProperties trustlistConfig;

    private MockRestServiceServer mockServer;

    @MockitoSpyBean
    private TrustlistService trustlistLogic;

    @Mock
    TokenStatuslistService tokenStatuslistService;

    @Mock
    JsonMapper jsonMapper;

    @Mock
    private VerificationTransactionService verificationService;

    @Autowired
    private OpenID4VPResponseService openID4VPResponseService;


    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient trustlistRestClient = builder.build();
        openID4VPResponseService = new OpenID4VPResponseService(verificationService,tokenStatuslistService, jsonMapper, trustlistRestClient, trustlistConfig);
        trustlistLogic = new TrustlistService(trustlistRestClient, trustlistConfig);
    }
}
