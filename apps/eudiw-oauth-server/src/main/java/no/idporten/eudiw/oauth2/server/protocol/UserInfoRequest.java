package no.idporten.eudiw.oauth2.server.protocol;

import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.oauth2.server.util.MultiValuedMapUtils.*;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"parameters"})
@ToString(exclude = {"parameters"})
public class UserInfoRequest implements AuditDataProvider {

    private transient String authorizationHeader;
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private Map<String, String> parameters = new HashMap<>();

    public UserInfoRequest(final Map<String, List<String>> headers, final Map<String, List<String>> parameters) {
        Map<String, List<String>> ciHeaders = caseInsensitiveMap(headers);
        authorizationHeader = getFirstValue("Authorization", ciHeaders);
        this.parameters = toMap(parameters);
    }

    public String getParameter(String parameterName) {
        return parameters.get(parameterName);
    }

    public String getBearerToken() {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
                .accessToken(getBearerToken())
                .build();
    }

}
