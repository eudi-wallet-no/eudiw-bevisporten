package no.idporten.eudiw.login.openid4vp.protocol;

import net.minidev.json.JSONObject;

public class VpFormat extends JSONObject {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final VpFormat vpFormat = new VpFormat();

        public Builder claimFormat(String credentialFormatIdentifier, FormatParameters formatParameters) {
            vpFormat.appendField(credentialFormatIdentifier, formatParameters);
            return this;
        }

        public VpFormat build() {
            return vpFormat;
        }
    }

}
