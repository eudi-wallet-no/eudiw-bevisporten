package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.openid.connect.sdk.Nonce;
import jakarta.validation.constraints.NotNull;

@NotNull
public record SessionRecordElements(Nonce nonce, Audience aud) {
}
