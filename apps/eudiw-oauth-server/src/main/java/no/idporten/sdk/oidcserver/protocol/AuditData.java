package no.idporten.sdk.oidcserver.protocol;

import lombok.*;

import java.util.Map;
import java.util.stream.Collectors;

@Builder
@Getter
@ToString
@AllArgsConstructor
public class AuditData {

    @Singular("attribute")
    private Map<String, Object> attributes;

    public static class AuditDataBuilder {

        protected String maskToken(String token) {
            if (token == null) {
                return token;
            }
            if (token.contains(".")) { // jwt
                return token.substring(0, token.lastIndexOf('.')) + "...";
            }
            if (token.length() > 10) { //opaque
                return token.substring(0, 10) + "...";
            }
            return token;
        }

        public AuditDataBuilder accessToken(String accessToken) {
            attribute("access_token", maskToken(accessToken));
            return this;
        }
    }

    public Object getAttribute(String attribute) {
        return attributes.get(attribute);
    }

    public Map<String, Object> getAttributes() {
        return attributes.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
