package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.issuance.CredentialIssuanceType;
import org.junit.jupiter.api.Test;

import static no.idporten.eudiw.issuer.TestData.bindingKeys;
import static no.idporten.eudiw.issuer.TestData.junitCredentialConfiguration;
import static no.idporten.eudiw.issuer.TestData.junitIssuerTenant;
import static no.idporten.eudiw.issuer.TestData.walletInstanceAttestationJwt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CredentialIssueContextTest {

    @Test
    void shouldExtractWalletInstanceAttestationFromAccessToken() {
        String wia = walletInstanceAttestationJwt().serialize();
        JWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().subject("junit").claim("wia", wia).build());

        CredentialIssueContext context = createContext(accessToken);

        assertEquals(wia, context.walletInstanceAttestation().jwt().serialize());
    }

    @Test
    void shouldHaveNoWalletInstanceAttestationWhenAccessTokenHasNone() {
        JWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().subject("junit").build());

        CredentialIssueContext context = createContext(accessToken);

        assertNull(context.walletInstanceAttestation());
    }

    private CredentialIssueContext createContext(JWT accessToken) {
        return new CredentialIssueContext(
                accessToken,
                junitIssuerTenant(),
                junitCredentialConfiguration(),
                null,
                CredentialIssuanceType.AUTHORIZATION_CODE,
                bindingKeys());
    }

}
