package no.idporten.eudiw.verifier.statuslist;

import net.eidas2sandkasse.tsl.TokenStatusListValidator;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("When checking token status lists")
@ExtendWith(MockitoExtension.class)
class TokenStatuslistServiceTest {

    @Mock
    private TokenStatusListValidator tokenStatusListValidator;

    @InjectMocks
    private TokenStatuslistService service;

    @DisplayName("then the uri and index is passed to the validator and a positive test result is handled")
    @Test
    void testValidStatusLookup() {
        URI uri = URI.create("https://status.eidas2sandkasse.dev/1");
        int idx = 1337;
        when(tokenStatusListValidator.validate(uri, idx)).thenReturn(true);
        ValidationStatus validationStatus = service.lookupStatusFromTokenStatuslist(uri, idx);
        assertEquals(ValidationStatus.VALID, validationStatus);
        verify(tokenStatusListValidator).validate(uri, idx);
    }

    @DisplayName("then the uri and index is passed to the validator and a negative result is handled")
    @Test
    void testRevokedStatusLookup() {
        URI uri = URI.create("https://status.eidas2sandkasse.dev/1");
        int idx = 1337;
        ValidationStatus validationStatus = service.lookupStatusFromTokenStatuslist(uri, idx);
        assertEquals(ValidationStatus.INVALID, validationStatus);
        verify(tokenStatusListValidator).validate(uri, idx);
    }

}
