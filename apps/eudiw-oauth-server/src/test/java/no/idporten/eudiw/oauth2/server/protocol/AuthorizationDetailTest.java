package no.idporten.eudiw.oauth2.server.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("When handling an authorization detail")
public class AuthorizationDetailTest {

    @DisplayName("then common attributes are added with correct attribute names")
    @Test
    public void testWriteReadCommonAttributes() {
        AuthorizationDetail authorizationDetail = new AuthorizationDetail();
        authorizationDetail.setType("t1");
        authorizationDetail.setResource("r1");
        assertAll(
                () -> assertEquals("t1", authorizationDetail.getType()),
                () -> assertEquals("r1", authorizationDetail.getResource())
        );
    }

    @DisplayName("then locations attribute is returned as a list")
    @Test
    public void testGetLocations() {
        AuthorizationDetail authorizationDetail = new AuthorizationDetail();
        authorizationDetail.setAttribute(AuthorizationDetail.ATTRIBUTE_LOCATIONS, (java.io.Serializable) List.of("https://issuer.example.com", "https://issuer2.example.com"));
        List<String> locations = authorizationDetail.getLocations();
        assertAll(
                () -> assertEquals(2, locations.size()),
                () -> assertEquals("https://issuer.example.com", locations.get(0)),
                () -> assertEquals("https://issuer2.example.com", locations.get(1))
        );
    }

    @DisplayName("then locations attribute is null when not set")
    @Test
    public void testGetLocationsWhenNotSet() {
        AuthorizationDetail authorizationDetail = new AuthorizationDetail();
        assertNull(authorizationDetail.getLocations());
    }

    @DisplayName("then TYPE_OPENID_CREDENTIAL constant has expected value")
    @Test
    public void testTypeOpenidCredentialConstant() {
        assertEquals("openid_credential", AuthorizationDetail.TYPE_OPENID_CREDENTIAL);
    }

}
