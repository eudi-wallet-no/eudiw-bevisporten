package no.idporten.eudiw.connector.authoritativesources.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CredentialData extends HashMap<String, Object> {
    public void addNumberList(String key, List<Number> list) {
        put(key, list);
    }

    public void addNumberMap(String key, Map<String, Number> map) {
        put(key, map);
    }

    public void addStringMap(String key, Map<String, String> map) {
        put(key, map);
    }

    public void addString(String key, String value) {
        put(key, value);
    }

    public void addBoolean(String key, Boolean value) {
        put(key, value);
    }
}
