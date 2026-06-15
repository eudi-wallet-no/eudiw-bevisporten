package no.idporten.eudiw.login.openid4vp.protocol;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;

import java.util.List;

public class SDJwtVCFormatParameters extends FormatParameters {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SDJwtVCFormatParameters formatParameters = new SDJwtVCFormatParameters();

        public Builder sdJwtAlgValues(List<JWSAlgorithm> algValues) {
            formatParameters.appendField("sd-jwt_alg_values", algValues.stream().map(Algorithm::getName).toList());
            return this;
        }

        public Builder kbJwtAuthAlgValues(List<JWSAlgorithm> algValues) {
            formatParameters.appendField("kb-jwt_alg_values", algValues.stream().map(Algorithm::getName).toList());
            return this;
        }

        public SDJwtVCFormatParameters build() {
            return formatParameters;
        }
    }

}
