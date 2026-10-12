package no.idporten.eudiw.verifier.certificateauthority;

import com.nimbusds.jose.util.X509CertUtils;
import no.idporten.eudiw.verifier.VerificationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.security.cert.X509Certificate;

@Service
public class CertificateAuthorityService {

    private final RestClient certificateAuthorityRestClient;
    private final CertificateAuthorityConfig certificateAuthorityConfig;

    public CertificateAuthorityService(@Qualifier("certificateAuthority") RestClient certificateAuthorityRestClient, CertificateAuthorityConfig certificateAuthorityConfig) {
        this.certificateAuthorityRestClient = certificateAuthorityRestClient;
        this.certificateAuthorityConfig = certificateAuthorityConfig;
    }

    public X509Certificate getCertificate() {
        CertificateAuthorityCertificate root =  certificateAuthorityRestClient.get()
                .uri(certificateAuthorityConfig.certificateAuthorityUrl().toString())
                .retrieve().body(CertificateAuthorityCertificate.class);
        try {
            return X509CertUtils.parse(root.certificate());
        } catch (Exception e) {
            throw new VerificationException("Failed to parse certificate from Certificate Authority", "invalid_request", e);
        }
    }
}
