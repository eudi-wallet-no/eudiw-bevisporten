package no.idporten.sdk.oidcserver.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
