package no.idporten.sdk.oidcserver.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * An OAuth2 authorization detail, from the JSON array in OAuth2 authorization_details parameter
 */
public class AuthorizationDetail extends HashMap<String, Serializable> {

    public static final String ATTRIBUTE_TYPE = "type";
    public static final String ATTRIBUTE_RESOURCE = "resource";

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

    /**
     * Gets an optional attribyte by name.
     * @param attribute attribute name
     * @return attributer value
     */
    public Serializable getAttribute(String attribute) {
        return get(attribute);
    }

}
