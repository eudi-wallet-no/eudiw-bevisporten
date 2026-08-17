package no.idporten.eudiw.oauth2.server.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An OAuth2 authorization detail, from the JSON array in OAuth2 authorization_details parameter
 */
public class AuthorizationDetail extends HashMap<String, Serializable> {

    public static final String ATTRIBUTE_TYPE = "type";
    public static final String ATTRIBUTE_RESOURCE = "resource";
    public static final String ATTRIBUTE_LOCATIONS = "locations";
    public static final String TYPE_OPENID_CREDENTIAL = "openid_credential";

    public AuthorizationDetail() {
        super();
    }

    public AuthorizationDetail(Map<String, Serializable> attributes) {
        super(attributes);
    }

    public void setAttribute(String attribute, Object value) {
        this.put(attribute, (Serializable) value);
    }

    /**
     * Gets the required attribute type.
     * @return type
     */
    public String getType() {
        return (String) get(ATTRIBUTE_TYPE);
    }

    public void setType(String type) {
        setAttribute(ATTRIBUTE_TYPE, type);
    }

    /**
     * Gets the optional attribute resource.
     * @return resource
     */
    public String getResource() {
        return (String) get(ATTRIBUTE_RESOURCE);
    }

    public void setResource(String resource) {
        setAttribute(ATTRIBUTE_RESOURCE, resource);
    }

    public List<String> getLocations() {
        Serializable raw = get(ATTRIBUTE_LOCATIONS);
        if (raw instanceof List<?> list && list.stream().allMatch(String.class::isInstance)) {
            return list.stream().map(String.class::cast).toList();
        }
        return null;
    }

    /**
     * Gets an optional attribyte by name.
     * @param attribute attribute name
     * @return attributer value
     */
    public Serializable getAttribute(String attribute) {
        return get(attribute);
    }

}
