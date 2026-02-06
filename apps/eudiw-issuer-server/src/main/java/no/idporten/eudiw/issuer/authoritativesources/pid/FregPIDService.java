package no.idporten.eudiw.issuer.authoritativesources.pid;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.digdir.freg.audit.AuditLog;
import no.digdir.freg.eventlog.EventLog;
import no.digdir.freg.service.FregResultMapper;
import no.digdir.logging.event.EventLogger;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class FregPIDService {

    private final FregIntegration fregIntegration;
    private final AuditLogger auditLogger;
    private final EventLogger eventLogger;


    @Autowired
    public FregPIDService(FregIntegration fregIntegration, AuditLogger auditLogger, EventLogger eventLogger) {
        this.fregIntegration = fregIntegration;
        this.auditLogger = auditLogger;
        this.eventLogger = eventLogger;
    }

    @Bean
    public no.digdir.freg.service.FregService fregService() {
        return new no.digdir.freg.service.FregService(
                new FregResultMapper(),
                new AuditLog(auditLogger),
                new EventLog(eventLogger),
                // TODO venter på Jackson 3 i freg lib
                new ObjectMapper(),
                fregIntegration);
    }

}
