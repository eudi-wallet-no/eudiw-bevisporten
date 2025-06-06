package no.idporten.sdk.oidcserver.protocol;

import no.idporten.sdk.oidcserver.util.JsonUtils;

import java.util.Map;

public interface JsonResponse {

    Map<String, Object> toJsonObject();

    default String toJsonString() {
        return JsonUtils.toJsonString(toJsonObject());
    }

}
