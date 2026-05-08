package no.idporten.eudiw.oauth2.server.protocol;

import no.idporten.eudiw.oauth2.server.util.StringUtils;

/**
 * Request support for https://www.rfc-editor.org/rfc/rfc8707.html
 */
public interface ResourceIndicatorSupport {

    String getResource();

    default boolean hasResourceIndicator() {
        return StringUtils.hasText(getResource());
    }

}
