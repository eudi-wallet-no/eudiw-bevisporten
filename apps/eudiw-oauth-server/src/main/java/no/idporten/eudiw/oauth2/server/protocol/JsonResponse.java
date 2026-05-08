package no.idporten.eudiw.oauth2.server.protocol;

import no.idporten.eudiw.oauth2.server.util.JsonUtils;

import java.util.Map;

public interface JsonResponse {

    Map<String, Object> toJsonObject();

    default String toJsonString() {
        return JsonUtils.toJsonString(toJsonObject());
    }

}
