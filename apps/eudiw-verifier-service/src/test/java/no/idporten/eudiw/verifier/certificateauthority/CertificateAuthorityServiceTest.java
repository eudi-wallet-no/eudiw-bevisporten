package no.idporten.eudiw.verifier.certificateauthority;

import no.idporten.eudiw.verifier.testdata.Certificates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("when testing CertificateAuthorityService")
public class CertificateAuthorityServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    private CertificateAuthorityService service;

    private Certificates certificates = new Certificates();

    @BeforeEach
    void setup() {
        service = new CertificateAuthorityService(restClient, config());
    }

    @DisplayName("Should fetch certificate from certificate authority")
    @Test
    void shouldFetchCertificate() {
        when(restClient.get()
                .uri(config().certificateAuthorityUrl().toString())
                .retrieve()
                .body(CertificateAuthorityCertificate.class))
                .thenReturn(new CertificateAuthorityCertificate("-----BEGIN CERTIFICATE-----" + certificates.rootCertificate() + "-----END CERTIFICATE-----"));

        X509Certificate rootCertificate = service.getCertificate();
        assertNotNull(rootCertificate);

    }

    private static CertificateAuthorityConfig config() {
        return new CertificateAuthorityConfig(Duration.ofSeconds(3), Duration.ofSeconds(3), URI.create("https://example.com"));
    }
}
