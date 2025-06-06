package no.idporten.sdk.oidcserver.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling authorizations")
class AuthorizationTest {

    @Test
    @DisplayName("then serializing to cache hides secrets")
    void testSerializationHidesSecrets(@TempDir File folder) throws Exception {
        Map<String, Serializable> extra = new HashMap<>();
        extra.put("coop", "extra");
        extra.put("rema", 1000);
        Authorization authorization = Authorization.builder()
                .aud("client")
                .code("c")
                .amr("a")
                .acr("r")
                .sub("p")
                .nonce("n")
                .attributes(extra)
                .build();
        authorization.setLifetimeSeconds(10);
        final File serializedObjectFile = new File(folder, "temp.ser");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serializedObjectFile))) {
            oos.writeObject(authorization);
        }
        final Authorization deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedObjectFile))) {
            deserialized = (Authorization) ois.readObject();
        }
        assertAll(
                () -> assertEquals(authorization, deserialized),
                () -> assertTrue(authorization.isValidNow())
        );
    }

    @Test
    @DisplayName("then audit data contains core and additional attributes")
    void testAuditData() {
        Map<String, Serializable> extra = new HashMap<>();
        extra.put("coop", "extra");
        extra.put("rema", 1000);
        Authorization authorization = Authorization.builder()
                .aud("client")
                .code("c")
                .amr("a")
                .acr("r")
                .sub("p")
                .attribute("foo", "bar")
                .attribute("boolean", true)
                .attributes(extra)
                .build();
        AuditData auditData = authorization.getAuditData();
        assertAll(
                () -> assertEquals(8, auditData.getAttributes().size()),
                () -> assertEquals("client", auditData.getAttribute("aud")),
                () -> assertEquals("p", auditData.getAttribute("sub")),
                () -> assertEquals("a", auditData.getAttribute("amr")),
                () -> assertEquals("r", auditData.getAttribute("acr")),
                () -> assertEquals("bar", auditData.getAttribute("foo")),
                () -> assertEquals(true, auditData.getAttribute("boolean")),
                () -> assertEquals(1000, auditData.getAttribute("rema")),
                () -> assertEquals("extra", auditData.getAttribute("coop")),
                () -> assertNull(auditData.getAttribute("code"))
        );
    }

}
