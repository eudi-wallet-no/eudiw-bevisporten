package no.idporten.eudiw.statuslist.service;

import no.idporten.eudiw.statuslist.domain.StatusEntry;
import no.idporten.eudiw.statuslist.issuer.api.StatusEntryUpdateRequest;
import no.idporten.eudiw.statuslist.issuer.config.StatusIssuerProperties;
import no.idporten.eudiw.statuslist.logging.audit.AuditEntryCollection;
import no.idporten.eudiw.statuslist.logging.audit.AuditService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.Deflater;

import static no.idporten.eudiw.statuslist.util.StatusListUtil.buildUri;
import static no.idporten.eudiw.statuslist.util.StatusListUtil.getListId;


@Service
public class StatusListService {

    private final AuditService auditService;
    private final StatusListRepository statusListRepository;
    private final StatusIssuerProperties statusIssuerProperties;

    public StatusListService(AuditService auditService, StatusListRepository statusListRepository, StatusIssuerProperties statusIssuerProperties) {
        this.auditService = auditService;
        this.statusListRepository = statusListRepository;
        this.statusIssuerProperties = statusIssuerProperties;
    }

    static byte[] compressZlib(byte[] input) {
        // BEST_COMPRESSION: level 9 is highest; nowrap=false (default) includes ZLIB headers (RFC 1950)

        ByteArrayOutputStream outputStream;
        try (Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION)) {
            outputStream = new ByteArrayOutputStream(input.length);
            byte[] buffer = new byte[1024];
            deflater.setInput(input);
            deflater.finish();
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
        }
        return outputStream.toByteArray();
    }

    public String getJsonStatuslist(String id) {
        StatusList statusList = statusListRepository.getStatusList(id);
        byte[] packedBytes = packStatuses(statusList);
        byte[] compressed = compressZlib(packedBytes);
        return HexFormat.of().formatHex(compressed);
    }


    public String getStatus(String id, int index) {
        int status = statusListRepository.getStatusList(id).getStatus(index);
        return Status.getStatus(status);
    }

    public List<StatusEntry> allocateToStatusList(int count) {
        List<StatusEntry> allocatedIndexes = new ArrayList<>(count);
        allocatedIndexes = recursiveAllocateToStatusList(count, allocatedIndexes);
        auditService.logAllocatedEntries(new AuditEntryCollection<>(allocatedIndexes));
        return allocatedIndexes;
    }

    public void revokeStatuses(List<StatusEntryUpdateRequest> entryUpdates) {
        for (StatusEntryUpdateRequest entry : entryUpdates) {
            updateStatus(getListId(entry.uri()), entry.idx(), Status.INVALID);
        }

        auditService.logUpdatedEntries(new AuditEntryCollection<>(entryUpdates), Status.getStatus(Status.INVALID));
    }

    public void updateStatus(String id, int index, int status) {
        if (!List.of(Status.VALID, Status.INVALID).contains(status)) {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }

        StatusList statusList = statusListRepository.getStatusList(id);

        statusList.addStatus(index, status);
    }

    private byte[] packStatuses(StatusList statusList) {
        int size = statusList.getStatusList().keySet().stream()
                .max(Integer::compareTo)
                .map(maxIndex -> maxIndex + 1)
                .orElse(0);

        int bitsPerStatus = statusList.getBitsPerStatus();
        byte[] packed = new byte[(size * bitsPerStatus + 7) / 8];
        int maxStatusValue = (1 << bitsPerStatus) - 1;

        for (int index = 0; index < size; index++) {
            int statusValue = statusList.getStatus(index);
            if (statusValue > maxStatusValue) {
                throw new IllegalArgumentException(
                        "Status value %d at index %d does not fit in %d bits".formatted(statusValue, index, bitsPerStatus)
                );
            }

            int bitOffset = index * bitsPerStatus;
            for (int bitIndex = 0; bitIndex < bitsPerStatus; bitIndex++) {
                if ((statusValue & (1 << bitIndex)) == 0) {
                    continue;
                }

                int absoluteBitIndex = bitOffset + bitIndex;
                int byteIndex = absoluteBitIndex / 8;
                int bitIndexInByte = absoluteBitIndex % 8;
                packed[byteIndex] |= (byte) (1 << bitIndexInByte);
            }
        }
        return packed;
    }

    private List<StatusEntry> recursiveAllocateToStatusList(int count, List<StatusEntry> statusEntries) {
        if (statusEntries.size() >= count) {
            return statusEntries;
        }

        int remainingCount = count - statusEntries.size();
        StatusList statusList = statusListRepository.getNextFreeStatusList();
        List<Integer> allocated = statusList.allocateToStatusList(remainingCount);

        for (Integer idx : allocated) {
            URI uri = buildUri(statusIssuerProperties.uri(), statusList.getId());
            statusEntries.add(new StatusEntry(idx, uri));
        }

        return recursiveAllocateToStatusList(count, statusEntries);
    }
}
