package no.idporten.sdk.oidcserver.protocol;

import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.idporten.sdk.oidcserver.util.MultiValuedMapUtils.*;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"parameters"})
@ToString(exclude = {"parameters"})
public class TokenRequest implements AuthenticatedRequest, AuditDataProvider {

    private transient String authorizationHeader;
    private transient String clientSecret;
    private transient String clientAssertion;
    private transient String clientAssertionType;
    private String clientId;
    private String codeVerifier;
    private String code;
    private String grantType;
    private String redirectUri;
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private Map<String, String> parameters = new HashMap<>();

    public TokenRequest(final Map<String, List<String>> headers, final Map<String, List<String>> parameters) {
        Map<String, List<String>> ciHeaders = caseInsensitiveMap(headers);
        authorizationHeader = getFirstValue("Authorization", ciHeaders);
        clientId = getFirstValue("client_id", parameters);
        codeVerifier = getFirstValue("code_verifier", parameters);
        clientSecret = getFirstValue("client_secret", parameters);
        clientAssertion = getFirstValue("client_assertion", parameters);
        clientAssertionType = getFirstValue("client_assertion_type", parameters);
        code = getFirstValue("code", parameters);
        grantType = getFirstValue("grant_type", parameters);
        redirectUri = getFirstValue("redirect_uri", parameters);
        this.parameters = toMap(parameters);
    }

    public String getParameter(String parameterName) {
        return parameters.get(parameterName);
    }

    @Override
    public void clearAuthentication() {
        clientSecret = null;
        authorizationHeader = null;
        clientAssertion = null;
    }

    @Override
    public void setAuthenticatedClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
                .attribute("client_id", clientId)
                .attribute("code_verifier", codeVerifier)
                .attribute("code", code)
                .attribute("grant_type", grantType)
                .attribute("redirect_uri", redirectUri)
                .build();
    }

}
