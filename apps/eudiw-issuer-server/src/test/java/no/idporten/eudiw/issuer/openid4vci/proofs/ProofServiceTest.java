package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jose.JWSAlgorithm;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.protocol.Proof;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class ProofServiceTest {

    @Spy
    CredentialIssuerServerProperties credentialIssuerServerProperties;

    @InjectMocks
    ProofService proofService;

    Proof createJWTProof(String jwt) {
        Proof proof = new Proof();
        proof.setProofType(ProofService.PROOF_TYPE_JWT);
        proof.setJwt(jwt);
        return proof;
    }

    @BeforeEach
    void setUpProperties() {
        credentialIssuerServerProperties.setProofSigningAlgorithms(List.of(JWSAlgorithm.ES256.getName()));
        credentialIssuerServerProperties.setCredentialIssuer(URI.create("https://credential-issuer.example.com"));
    }

    @Test
    void testValidProofFromAndroidWallet() throws Exception {
       String jwt = "eyJ0eXAiOiJvcGVuaWQ0dmNpLXByb29mK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiblVXQW9BdjNYWml0aDhFN2kxOU9kYXhPTFlGT3dNLVoyRXVNMDJUaXJUNCIsInkiOiJIc2tIVThCalVpMVU5WHFpN1N3bWo4Z3dBS18weGtjRGpFV183MVNvc0VZIn19.eyJhdWQiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUuY29tIiwiaWF0IjoxNzAxOTYwNDQ0LCJub25jZSI6IkxhclJHU2JtVVBZdFJZTzZCUTR5bjgifQ.-a3EDsxClUB4O3LeDD5DVGEnNMT01FCQW4P6-2-BNBqc_Zxf0Qw4CWayLEpqkAomlkLb9zioZoipdP-jvh1WlA";
       Proof proof = createJWTProof(jwt);
       proofService.validateProof(proof);
       assertNotNull(proof.getBindingKey());
    }

}
