package no.idporten.sdk.oidcserver.protocol;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling userinfo requests")
public class UserInfoRequestTest {

    @Test
    @DisplayName("then core parameters and headers can be parsed")
    public void testCoreParametersAndHeaders() {
        final String authorizationHeader = "Bearer xxxyyyzzz";

        UserInfoRequest userInfoRequest = new UserInfoRequest(Map.of("Authorization", Collections.singletonList(authorizationHeader)), Collections.emptyMap());
        assertAll(
                () -> assertEquals(authorizationHeader, userInfoRequest.getAuthorizationHeader()),
                () -> assertEquals("xxxyyyzzz", userInfoRequest.getBearerToken())
        );
    }

    @Test
    @DisplayName("then additional parameters can be parsed")
    public void testAdditionalParameters() {
        final String extra = "xxx";

        UserInfoRequest userInfoRequest = new UserInfoRequest(Collections.emptyMap(), Map.of("extra", Collections.singletonList(extra)));
        assertEquals(extra, userInfoRequest.getParameter("extra"));
    }

    @Test
    @DisplayName("then audit data contains the masked bearer token")
    public void testAuditData() {
        final String authorizationHeader = "Bearer xxx.yyy.zzz";

        UserInfoRequest userInfoRequest = new UserInfoRequest(Map.of("Authorization", Collections.singletonList(authorizationHeader)), Collections.emptyMap());
        AuditData auditData = userInfoRequest.getAuditData();
        assertEquals("xxx.yyy...", auditData.getAttribute("access_token"));
    }

}
