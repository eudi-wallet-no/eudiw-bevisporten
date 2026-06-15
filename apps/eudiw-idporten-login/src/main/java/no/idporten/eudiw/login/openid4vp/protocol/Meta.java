package no.idporten.eudiw.login.openid4vp.protocol;

public class Meta extends ProtocolJSONObject {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Meta meta = new Meta();

        public Builder doctypeValue(String doctypeValue) {
            meta.appendField("doctype_value", doctypeValue);
            return this;
        }

        public Meta build() {
            return meta;
        }
    }

}
