package no.idporten.eudiw.login.openid4vp.protocol;

import java.util.List;

public class Claim extends ProtocolJSONObject {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Claim claim = new Claim();

        public Builder path(List<String> pathElements) {
            for (String pathElement : pathElements) {
                claim.appendArrayField("path", pathElement);
            }
            return this;
        }

        public Claim build() {
            return claim;
        }
    }

}
