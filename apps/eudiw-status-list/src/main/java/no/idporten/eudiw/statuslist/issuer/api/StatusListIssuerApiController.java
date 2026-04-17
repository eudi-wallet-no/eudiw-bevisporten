package no.idporten.eudiw.statuslist.issuer.api;

import jakarta.validation.Valid;
import no.idporten.eudiw.statuslist.domain.StatusEntry;
import no.idporten.eudiw.statuslist.issuer.config.StatusIssuerProperties;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/status-issuer/api/v1/entries")
public class StatusListIssuerApiController {

    private final StatusIssuerProperties properties;

    public StatusListIssuerApiController(StatusIssuerProperties properties) {
        this.properties = properties;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StatusCreateResponse> allocateStatus(@RequestBody @Valid StatusCreateRequest request, @RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
        verifyApiKey(properties.apiKey(), apiKey);
        StatusCreateResponse response = new StatusCreateResponse(allocateStatuses(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void verifyApiKey(String serverKey, String inputKey) {
        if (serverKey == null) {
            LoggerFactory.getLogger(StatusListIssuerApiController.class).error("API key is missing in configuration.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }
        // Use MessageDigest because of security: Attacker can time String.equals() since it finishes on first mismatch in String, but MessageDigest always compares entire String.
        // So slower, but safer security.
        if (inputKey == null || !MessageDigest.isEqual(serverKey.getBytes(), inputKey.getBytes())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> revoke(@RequestBody @Valid StatusUpdateRequest request, @RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
        verifyApiKey(properties.apiKey(), apiKey);
        // TODO call service for update (revoke) status on status-list
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private static @NonNull List<StatusEntry> allocateStatuses(StatusCreateRequest request) {
        List<StatusEntry> statusEntries = new ArrayList<>(request.numberOfEntries());
        // TODO call service for allocate status on status-list, instead of dummy
        for (int i = 0; i < request.numberOfEntries(); i++) {
            StatusEntry e1 = new StatusEntry(i, "https://status.eidas2sandkasse.dev/1");
            statusEntries.add(e1);
        }
        return statusEntries;
    }
}
