package no.idporten.eudiw.statuslist.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import no.idporten.eudiw.statuslist.repository.StatusListRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricService {
    private final MeterRegistry meterRegistry;
    private final StatusListRepository statusListRepository;

    private final AtomicInteger freeListCount = new AtomicInteger(0);
    private final AtomicInteger fullListCount = new AtomicInteger(0);

    public MetricService(MeterRegistry meterRegistry, StatusListRepository statusListRepository) {
        this.meterRegistry = meterRegistry;
        this.statusListRepository = statusListRepository;

        meterRegistry.gauge("status_list_lists_not_full", freeListCount);
        meterRegistry.gauge("status_list_lists_full", fullListCount);
    }

    @Scheduled(cron = "0 * * * * *")
    public void reportDbStatus() {
        freeListCount.set(statusListRepository.getFreeListCount());
        fullListCount.set(statusListRepository.getFullListCount());
    }

    public void countStatusAllocation(int count) {
       meterRegistry.counter("status_list_issuer_status_allocated_entry_count").increment(count);
    }
}
