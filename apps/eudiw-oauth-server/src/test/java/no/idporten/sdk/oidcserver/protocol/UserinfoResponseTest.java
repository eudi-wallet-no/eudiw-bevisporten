package no.idporten.sdk.oidcserver.protocol;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When generating userinfo responses")
public class UserinfoResponseTest {

    @Test
    @DisplayName("then the sub attribute is included")
    public void testBuildTokenResponse() {
        UserInfoResponse userInfoResponse = UserInfoResponse.builder().sub("s").build();
        assertEquals("s", userInfoResponse.getSub());
    }

    @Test
    @DisplayName("then audit data contains sub")
    public void testAuditData() {
        UserInfoResponse userInfoResponse = UserInfoResponse.builder().sub("s").build();
        AuditData auditData = userInfoResponse.getAuditData();
        assertAll(
                () -> assertEquals(1, auditData.getAttributes().size()),
                () -> assertEquals("s", auditData.getAttribute("sub"))
        );
    }

}
