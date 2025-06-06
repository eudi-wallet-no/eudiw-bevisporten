package no.idporten.sdk.oidcserver.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;


/**
 * ISO6523 identifier
 */
public record IdentifierISO6523(
        String authority,
        String ID
)

        implements Serializable {

    public static final String ISO6523_AUTHORITY = "iso6523-actorid-upis";
    public static final String ISO6523_NORWAY = "0192";
    @Serial
    private static final long serialVersionUID = 1071282133233281932L;


    public IdentifierISO6523(String id) {
        this(ISO6523_AUTHORITY, id);
    }

    public IdentifierISO6523(String authority, String ID) {
        this.authority = Objects.requireNonNull(authority, "authority cannot be null");
        if (Objects.requireNonNull(ID, "ID cannot be null").startsWith(ISO6523_NORWAY)) {
            this.ID = ID;
        } else {
            this.ID = formatID(ID);
        }
    }

    private static String formatID(String id) {
        return "%s:%s".formatted(ISO6523_NORWAY, id);
    }
}
