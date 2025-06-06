package no.idporten.sdk.oidcserver.util;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import com.nimbusds.jose.util.JSONObjectUtils;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Simple JSON utilities.  Attempts to isolate the SDK from JSON library implementation.
 * JSON objects are represented as Map<String, Object>.
 *
 * The current implementation rely on a GSON implementation embedded in Nimbus.
 */
public class JsonUtils {

    private static final Gson GSON;

    static {
        GSON = (new GsonBuilder()).serializeNulls().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).disableHtmlEscaping().create();
    }

    public static JsonObjectBuilder jsonObjectBuilder() {
        return JsonObjectBuilder.builder();
    }

    public static String toJsonString(Object jsonObject) {
        return GSON.toJson(jsonObject);
    }

    public static Map<String, Object> parseJsonObject(String s) throws ParseException {
        return JSONObjectUtils.parse(s);
    }

    public static List<Object> parseJsonArray(String s) throws ParseException {
        if (!StringUtils.hasText(s)) {
            throw new ParseException("Empty json input not allowed", 0);
        }
        try {
            Type type = new TypeToken<List<Object>>() {
            }.getType();
            return GSON.fromJson(s, type);
        } catch (Exception e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

}
