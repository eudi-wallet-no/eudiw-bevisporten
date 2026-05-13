package no.idporten.eudiw.oauth2.server.protocol;

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

        protected String maskJwt(String jwt) {
            if (jwt == null) {
                return jwt;
            }
            if (jwt.contains(".")) { // jwt
                return jwt.substring(0, jwt.lastIndexOf('.')) + "...";
            }
            if (jwt.length() > 10) { //opaque
                return jwt.substring(0, 10) + "...";
            }
            return jwt;
        }

        public AuditDataBuilder maskedJwt(String attribute, String jwt) {
            attribute(attribute, maskJwt(jwt));
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
