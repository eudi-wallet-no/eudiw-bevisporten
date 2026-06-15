package no.idporten.eudiw.login.openid4vp.protocol;

public class Credential extends ProtocolJSONObject {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Credential credential = new Credential();

        public Builder id(String id) {
            credential.appendField("id", id);
            return this;
        }

        public Builder format(String format) {
            credential.appendField("format", format);
            return this;
        }

        public Builder meta(Meta meta) {
            credential.appendField("meta", meta);
            return this;
        }

        public Builder claim(Claim claim) {
            credential.appendArrayField("claims", claim);
            return this;
        }

        public Credential build() {
            return credential;
        }
    }

}
