package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.TestData;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.credentials.types.StringValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static no.idporten.eudiw.issuer.TestData.credentialIssuerTenant;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

@DisplayName("When using authorized claims sources")
public class AuthorizedClaimsSourceTest {

    static class AuthorizedJUnitClaimsSource extends AbstractAuthorizedClaimsSource {

        @Override
        public List<Claim> issueClaims(CredentialIssueContext credentialIssueContext) {
            return Collections.singletonList(new Claim(Collections.singletonList("c"), new StringValue("v")));
        }
    }

    @DisplayName("then authorized claims are issued")
    @Test
    public void testIssueClaimsSource() {
        AuthorizedClaimsSource claimsSource = spy(new AuthorizedJUnitClaimsSource());
        String fnr = "12345678910";
        List<Claim> claims = claimsSource.issueClaims(new CredentialIssueContext(TestData.accessToken(fnr), credentialIssuerTenant("junit"),null));
        assertAll(
                () -> assertEquals(1, claims.size()),
                () -> assertEquals("c", claims.getFirst().getPath().getFirst()),
                () -> assertEquals("v", ((StringValue) claims.getFirst().getValue()).value())
        );
    }

}
