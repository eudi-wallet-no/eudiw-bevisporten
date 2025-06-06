package no.idporten.sdk.oidcserver.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JsonObjectBuilder {

    private JsonObjectBuilder() {}

    private Map<String, Object> jsonObject = new HashMap<>();

    public static JsonObjectBuilder builder() {
        return new JsonObjectBuilder();
    }

    /**
     * Adds an attribute.  Ignores null values.
     * @param name attribute name
     * @param value attribute value
     * @return this builder
     */
    public JsonObjectBuilder addAttribute(String name, Object value) {
        Objects.requireNonNull(name);
        if (value != null) {
            jsonObject.put(name, value);
        }
        return this;
    }

    /**
     * Adds an attribute.  Ignores null values and empty collections.
     * @param name attribute name
     * @param value attribute value
     * @return this builder
     */
    public JsonObjectBuilder addAttribute(String name, Collection value) {
        Objects.requireNonNull(name);
        if (value != null && ! value.isEmpty()) {
            jsonObject.put(name, value);
        }
        return this;
    }

    public Map<String, Object> build() {
        return new HashMap<>(jsonObject);
    }

}
