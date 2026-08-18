package no.idporten.eudiw.login.openid4vp;

import java.net.URI;

public record OpenID4VPAuthorizationRequests(
        URI sameDeviceRequest,
        URI crossDeviceQRCodeDataUri
) {
}
