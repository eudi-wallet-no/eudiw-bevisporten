package no.idporten.eudiw.issuer.authoritativesources.pid;

import no.digdir.freg.audit.AuditLog;
import no.digdir.freg.eventlog.EventLog;
import no.digdir.freg.service.FregResultMapper;
import no.digdir.logging.event.EventLogger;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class FregPIDService {

    private final FregIntegration fregIntegration;
    private final AuditLogger auditLogger;
    private final EventLogger eventLogger;
    private final JsonMapper jsonMapper;


    @Autowired
    public FregPIDService(FregIntegration fregIntegration, AuditLogger auditLogger, EventLogger eventLogger, JsonMapper jsonMapper) {
        this.fregIntegration = fregIntegration;
        this.auditLogger = auditLogger;
        this.eventLogger = eventLogger;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public no.digdir.freg.service.FregService fregService() {
        return new no.digdir.freg.service.FregService(
                new FregResultMapper(),
                new AuditLog(auditLogger),
                new EventLog(eventLogger),
                jsonMapper,
                fregIntegration);
    }

}
