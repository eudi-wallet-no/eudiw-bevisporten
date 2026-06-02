package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.openid4vci.protocol.Proofs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ProofServiceTest {

    @Spy
    CredentialIssuerServerProperties credentialIssuerServerProperties;

    @InjectMocks
    ProofService proofService;


    Proofs createJWTProofs(String... jwts) {
        Proofs proof = new Proofs();
        proof.setJwt(List.of(jwts));
        return proof;
    }

    @BeforeEach
    void setUpProperties() {
        credentialIssuerServerProperties.setProofSigningAlgorithms(List.of(JWSAlgorithm.ES256.getName()));
        credentialIssuerServerProperties.setCredentialIssuer(URI.create("https://utsteder.test.eidas2sandkasse.net"));
    }


    @Test
    void testValidProofFromAndroidWallet() throws Exception {
       String jwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6Im9wZW5pZDR2Y2ktcHJvb2Yrand0IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiOWRvODhIMGdTQWhfd1YxQzRFbF90dHlBejBQSVlXR2dOclVjbFVSMUNuNCIsInkiOiJwRUNocDhVaWVZZm5mSXQ4cWlQUmNLbVRVRDZGdTBkaXgySFVXX2xBVXk4In19.eyJhdWQiOiJodHRwczovL3V0c3RlZGVyLnRlc3QuZWlkYXMyc2FuZGthc3NlLm5ldCIsImlhdCI6MTc2Mjk1MTI4NSwiaXNzIjoid2FsbGV0LWRldiIsIm5vbmNlIjoiZzVsSERxVVdId1hlOGNpQlF2ZU8yMjQ4YzVQS3JSZDZranRfZ3NvR1dvSSJ9.5RL_YVDljEoWCZxyxWA2y5t0yBpSyOl91Om8huBuyPEOtfrAqULD2rvy79K7XCU65ZeuB2ySLynd03u0xAJq8Q";
       Proofs proofs = createJWTProofs(jwt);
        CredentialIssuerTenant credentialIssuerTenant = CredentialIssuerTenant.builder()
                .credentialIssuer(URI.create("https://utsteder.test.eidas2sandkasse.net"))
                .build();
       List<JWK> bindingKeys = proofService.validateProofs(credentialIssuerTenant, proofs);
       assertEquals(1, bindingKeys.size());
    }

}
