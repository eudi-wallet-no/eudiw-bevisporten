package no.idporten.eudiw.issuer.issuance.preauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.nimbusds.oauth2.sdk.id.Identifier;

public class IssuanceTransactionId extends Identifier {

    public IssuanceTransactionId() {
        super();
    }

    @JsonCreator
    public IssuanceTransactionId(String value) {
        super(value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }

}
