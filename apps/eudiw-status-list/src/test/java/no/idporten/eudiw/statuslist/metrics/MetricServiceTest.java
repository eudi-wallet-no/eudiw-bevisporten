package no.idporten.eudiw.statuslist.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.idporten.eudiw.statuslist.repository.StatusListRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetricServiceTest {
    @Mock
    StatusListRepository statusListRepository;

    @Spy
    SimpleMeterRegistry meterRegistry;

    @InjectMocks
    MetricService metricService;


    @Test
    @DisplayName("Report database status-list status")
    void reportDbStatus_registersExpectedGauges() {
        when(statusListRepository.getFreeListCount()).thenReturn(7);
        when(statusListRepository.getFullListCount()).thenReturn(3);

        metricService.reportDbStatus();

        Gauge notFullGauge = meterRegistry.find("status_list_lists_not_full").gauge();
        Gauge fullGauge = meterRegistry.find("status_list_lists_full").gauge();

        assertNotNull(notFullGauge);
        assertNotNull(fullGauge);
        assertEquals(7.0, notFullGauge.value());
        assertEquals(3.0, fullGauge.value());

        verify(statusListRepository, times(1)).getFreeListCount();
        verify(statusListRepository, times(1)).getFullListCount();
    }

    @Test
    @DisplayName("Count number of status allocations (both api calls and actual status entry allocations)")
    void countStatusAllocation_incrementsExpectedCounters() {
        metricService.countStatusAllocation(4);

        assertEquals(4.0, meterRegistry.get("status_list_issuer_status_allocated_entry_count").counter().count());

        verifyNoInteractions(statusListRepository);
    }
}
