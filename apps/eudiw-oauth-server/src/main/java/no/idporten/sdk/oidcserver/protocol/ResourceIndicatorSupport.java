package no.idporten.sdk.oidcserver.protocol;

import no.idporten.sdk.oidcserver.util.StringUtils;

/**
 * Request support for https://www.rfc-editor.org/rfc/rfc8707.html
 */
public interface ResourceIndicatorSupport {

    String getResource();

    default boolean hasResourceIndicator() {
        return StringUtils.hasText(getResource());
    }

}
