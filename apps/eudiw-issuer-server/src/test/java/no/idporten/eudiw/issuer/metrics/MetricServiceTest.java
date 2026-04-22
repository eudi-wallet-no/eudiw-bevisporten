package no.idporten.eudiw.issuer.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MetricServiceTest {

    private MeterRegistry meterRegistry;

    private MetricService metricService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricService = new MetricService(meterRegistry);
    }

    @Test
    void testCountStartIssuance() {
        metricService.countStartIssuance("some_credential_mock_config_id");
        metricService.countStartIssuance("some_credential_mock_config_id");
        Assertions.assertEquals(2, metricService.getCount("app_credential_issue_start",  "some_credential_mock_config_id"));
    }

    @Test
    void testCountIssued() {
        metricService.countIssued("some_credential_mock_config_id");
        metricService.countIssued("some_credential_mock_config_id");
        Assertions.assertEquals(2, metricService.getCount("app_credential_issue_issued", "some_credential_mock_config_id"));
    }
}
