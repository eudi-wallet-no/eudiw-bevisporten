package no.idporten.eudiw.login.openid4vp.protocol;

import java.util.List;

public class MdocFormatParameters extends FormatParameters {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final MdocFormatParameters formatParameters = new MdocFormatParameters();

        public Builder issuerAuthAlgValues(List<String> issuerAuthAlgValues) {
            formatParameters.appendField("issuerauth_alg_values", issuerAuthAlgValues);
            return this;
        }

        public Builder deviceAuthAlgValues(List<String> deviceAuthAlgValues) {
            formatParameters.appendField("deviceauth_alg_values", deviceAuthAlgValues);
            return this;
        }

        public MdocFormatParameters build() {
            return formatParameters;
        }
    }

}
