package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.ErrorCode;
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
            throw new IssuerServerException(ErrorCode.INVALID_NOTIFICATION_ID, "Missing notification id.");
        }
        if (!StringUtils.hasText(event())) {
            throw new IssuerServerException(ErrorCode.INVALID_NOTIFICATION_REQUEST, "Missing event value.");
        }
        if (!event().matches("credential_accepted|credential_failure|credential_deleted")) {
            throw new IssuerServerException(ErrorCode.INVALID_NOTIFICATION_REQUEST, "Unknown event value.");
        }
        // TODO event description JIRA https://digdir.atlassian.net/browse/EUW-533
    }

}
