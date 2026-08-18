package no.idporten.eudiw.issuer.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricService {

    private final MeterRegistry meterRegistry;

    public MetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void countStartIssuance(String credentialConfigurationId) {
        meterRegistry.counter("app_credential_issue_start", "credentials", credentialConfigurationId).increment();
    }

    public void countIssued(String credentialConfigurationId) {
        meterRegistry.counter("app_credential_issue_issued", "credentials", credentialConfigurationId).increment();
    }

    protected double getCount(String name, String credentialConfigurationId) {
       return meterRegistry.get(name).tag("credentials", credentialConfigurationId).counter().count();
    }
}
