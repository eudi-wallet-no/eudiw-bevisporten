package no.idporten.sdk.oidcserver.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("When using a form_post response")
public class FormPostResponseTest {

    @Test
    @DisplayName("then response parameters are added to the redirect form")
    public void testBuildRedirectForm() {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .responseMode("form_post")
                .redirectUri("https://www.idporten.no/")
                .state("sss")
                .build();
        authorizationResponse.addParameter("empty", null);
        authorizationResponse.addParameter("any", "thing");
        FormPostResponse formPostResponse = new FormPostResponse(authorizationResponse.getRedirectUri(), authorizationResponse.toResponseParameters());
        String html = formPostResponse.getRedirectForm();
        assertAll(
                () -> assertTrue(html.contains("method=\"post\"")),
                () -> assertTrue(html.contains("action=\"https://www.idporten.no/\"")),
                () -> assertTrue(html.contains("name=\"any\" value=\"thing\"")),
                () -> assertTrue(html.contains("name=\"state\" value=\"sss\"")),
                () -> assertFalse(html.contains("name=\"empty\"")),
                () -> assertFalse(html.contains("name=\"aud\""))
        );
    }

}
