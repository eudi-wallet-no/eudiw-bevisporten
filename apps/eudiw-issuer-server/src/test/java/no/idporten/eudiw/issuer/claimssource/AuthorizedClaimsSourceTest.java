package no.idporten.eudiw.issuer.claimssource;


import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.credentials.types.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@DisplayName("When using authorized claims sources")
public class AuthorizedClaimsSourceTest {

    static class AuthorizedJUnitClaimsSource extends AbstractAuthorizedClaimsSource {
        @Override
        public List<Claim> pull(String pid) {
            return Collections.singletonList(new Claim(Collections.singletonList("c"), new StringValue("v")));
        }

        @Override
        public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
            return new DocumentMetadata(List.of(new DocumentMetadata.Display("no", "Junit")), List.of(new ClaimMetadata(ClaimMetadata.EMPTY_NAMESPACE, "b", ClaimDataType.STRING, Map.of("no", "B"), true, ".*")));
        }
    }


    @DisplayName("then authorized claims are issued")
    @Test
    public void testIssueClaimsSource() {
        AuthorizedClaimsSource claimsSource = spy(new AuthorizedJUnitClaimsSource());
        String fnr = "12345678910";
        List<Claim> claims = claimsSource.issueClaims(new CredentialIssueContext(createAccessToken(fnr), null));
        assertAll(
                () -> assertEquals(1, claims.size()),
                () -> assertEquals("c", claims.getFirst().getPath().getFirst()),
                () -> assertEquals("v", ((StringValue) claims.getFirst().getValue()).value())
        );
        verify(claimsSource).pull(eq(fnr));
    }

    @DisplayName("then authorized claims are not issued when fnr is not in token")
    @Test
    public void testIssueClaimsSourceWithInvalidToken() {
        AuthorizedClaimsSource claimsSource = spy(new AuthorizedJUnitClaimsSource());
        assertThrows(IssuerServerException.class, () -> claimsSource.issueClaims(new CredentialIssueContext(createAccessToken(null), null)));
        assertThrows(IssuerServerException.class, () -> claimsSource.issueClaims(new CredentialIssueContext(createAccessToken(" "), null)));
    }

    @NotNull
    public static JWT createAccessToken(String fnr) {
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder().subject(fnr).build();
        return new PlainJWT(claimSet);
    }

}
