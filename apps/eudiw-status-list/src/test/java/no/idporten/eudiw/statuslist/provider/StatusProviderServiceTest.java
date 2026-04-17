package no.idporten.eudiw.statuslist.provider;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(MockitoExtension.class)
public class StatusProviderServiceTest {

    @Spy
    private final StatusProviderProperties  statusProviderProperties = new StatusProviderProperties(
            URI.create("https://junit.eidas2sandkasse.dev"),
            Duration.ofHours(1),
            Duration.ofMinutes(47)
    );

    @InjectMocks
    private StatusProviderService statusProviderService;

    @Test
    @DisplayName("Should generate a valid Status List Token as JWT")
    void getValidStatusListTest() throws Exception {
        String testId = "1";

        JWT jwt = statusProviderService.getStatusList(testId);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();

        assertEquals("https://junit.eidas2sandkasse.dev/lists/1", claims.getSubject());
        assertEquals(47 * 60L, claims.getClaim("ttl"));

        Map<String, Object> statusList = claims.getJSONObjectClaim("status_list");
        assertEquals(1, statusList.get("bits"));
        assertEquals("eNrbuRgAAhcBXQ", statusList.get("lst"));


        Long iat = claims.getDateClaim("iat").toInstant().toEpochMilli() / 1000;
        Long exp = claims.getDateClaim("exp").toInstant().toEpochMilli() / 1000;
        assertEquals(60 * 60, exp - iat, 2);
    }

    @Test
    @DisplayName("Should return status list not found when calling non-existing status-list")
    void getNonExistingStatusListTest() throws Exception {
        String nonExistingId = "non-existing-id";

        assertThrows(StatusListNotFoundException.class, () -> {
            statusProviderService.getStatusList(nonExistingId);
        });
    }
}
