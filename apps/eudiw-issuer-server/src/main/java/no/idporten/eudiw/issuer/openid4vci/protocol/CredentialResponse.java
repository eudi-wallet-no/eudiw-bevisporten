package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
public class CredentialResponse {

    @JsonProperty("credentials")
    @Singular
    private List<Credential> credentials;

    @JsonProperty("notification_id")
    private NotificationId notificationId;

}
