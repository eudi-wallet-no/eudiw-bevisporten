package no.idporten.eudiw.issuer.openid4vci.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.nimbusds.oauth2.sdk.id.Identifier;

public class NotificationId extends Identifier {

    public NotificationId() {
        super();
    }

    @JsonCreator
    public NotificationId(String value) {
        super(value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}
