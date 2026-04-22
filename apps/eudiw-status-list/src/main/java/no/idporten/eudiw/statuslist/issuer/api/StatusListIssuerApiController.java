package no.idporten.eudiw.statuslist.issuer.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import no.idporten.eudiw.statuslist.domain.StatusEntry;
import no.idporten.eudiw.statuslist.exceptions.ErrorResponse;
import no.idporten.eudiw.statuslist.issuer.config.StatusIssuerProperties;
import no.idporten.eudiw.statuslist.service.Status;
import no.idporten.eudiw.statuslist.service.StatusListService;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/status-issuer/api/v1/entries")
@Tag(name = "StatusList Issuer API", description = "Status Issuer administrasjon av status-lister")
public class StatusListIssuerApiController {

    private final StatusIssuerProperties properties;
    private final StatusListService statuslistService;

    public StatusListIssuerApiController(StatusIssuerProperties properties, StatusListService statusListService) {
        this.properties = properties;
        this.statuslistService = statusListService;
    }

    @Operation(
            summary = "Alloker status på status-list",
            description = "Alloker N gyldige statuser på status-list")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Statuser allokert"),
            @ApiResponse(responseCode = "400", description = "Ugyldig forespørsel",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Ikkje gyldig api-key",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Intern feil",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(
            summary = "Oppdaterer status på status-list",
            description = "Oppdaterer status på indeksane angitt i requesten på status-list. Støtter berre revokering (INVALID) status-type p.t.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Statuser oppdatert"),
            @ApiResponse(responseCode = "400", description = "Ugyldig forespørsel",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Ikkje gyldig api-key",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Intern feil",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> revoke(@RequestBody @Valid StatusUpdateRequest request, @RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
        verifyApiKey(properties.apiKey(), apiKey);
        // TODO add listID
        for (StatusEntryUpdateRequest entry: request.statusListEntries()) {
            statuslistService.updateStatus(entry.idx(), Status.INVALID);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private @NonNull List<StatusEntry> allocateStatuses(StatusCreateRequest request) {
        List<StatusEntry> statusEntries = new ArrayList<>(request.numberOfEntries());
        List<Integer> allocatedStatuses = statuslistService.allocateToStatusList(request.numberOfEntries());
        String listId = "1"; // TODO get list id from statuslist service and URI
        URI uri = UriComponentsBuilder.fromUriString(properties.uri()).buildAndExpand(listId).toUri();
        for (Integer allocatedStatus: allocatedStatuses) {
            StatusEntry e1 = new StatusEntry(allocatedStatus, uri);
            statusEntries.add(e1);
        }
        return statusEntries;
    }
}
