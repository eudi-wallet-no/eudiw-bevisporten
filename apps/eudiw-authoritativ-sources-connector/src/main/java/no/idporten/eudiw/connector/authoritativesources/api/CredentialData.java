package no.idporten.eudiw.connector.authoritativesources.api;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CredentialData extends TreeMap<String, Object> {
    public <T extends Number> void addNumberList(String key, List<T> list) {
        put(key, list);
    }

    public <T extends  Number> void addNumberMap(String key, Map<String, T> map) {
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
