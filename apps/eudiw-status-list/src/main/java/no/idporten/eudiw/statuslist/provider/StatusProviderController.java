package no.idporten.eudiw.statuslist.provider;

import com.nimbusds.jwt.JWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "StatusList Provider API", description = "Status Provider gir ut status-list")
public class StatusProviderController {

    private static final String STATUS_LIST_TOKEN = "application/statuslist+jwt";
    private final StatusProviderService statusProviderService;

    public StatusProviderController(StatusProviderService statusProviderService) {
        this.statusProviderService = statusProviderService;
    }

    @Operation(
            summary = "Lister ut status-lister",
            description = "Status-list som JWT")
    @CrossOrigin(origins = "*", methods = RequestMethod.GET)
    @GetMapping(path = "/lists/{id}", produces = STATUS_LIST_TOKEN)
    public ResponseEntity<String> getStatusList(@PathVariable String id) {
        JWT jwt = statusProviderService.getStatusList(id);
        return ResponseEntity.ok(jwt.serialize());
    }
}
