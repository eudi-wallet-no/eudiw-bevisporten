package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationRequest(
        @JsonProperty("notification_id")
        String notificationId,
        @JsonProperty("event")
        String event,
        @JsonProperty("event_description")
        String eventDescription
) {

    public void validate() {
        if (!StringUtils.hasText(notificationId())) {
            throw new IssuerServerException("invalid_notification_id", "Missing notification id.", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(event())) {
            throw new IssuerServerException("invalid_notification_request", "Missing event value.", HttpStatus.BAD_REQUEST);
        }
        if (!event().matches("credential_accepted|credential_failure|credential_deleted")) {
            throw new IssuerServerException("invalid_notification_request", "Unknown event value.", HttpStatus.BAD_REQUEST);
        }
        // TODO event description
    }

}
