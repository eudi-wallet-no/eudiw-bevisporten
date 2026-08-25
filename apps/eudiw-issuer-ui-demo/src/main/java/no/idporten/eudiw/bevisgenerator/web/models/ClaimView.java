package no.idporten.eudiw.bevisgenerator.web.models;

import java.util.List;

/**
 * A single claim, formatted as display-ready text. If the underlying claim value was
 * a nested object (or a list of objects), {@code children} holds the nested claims and
 * {@code value} is null; the template renders children recursively as a nested list.
 */
public record ClaimView(String name, String value, List<ClaimView> children) {

    public boolean isGroup() {
        return children != null && !children.isEmpty();
    }
}
