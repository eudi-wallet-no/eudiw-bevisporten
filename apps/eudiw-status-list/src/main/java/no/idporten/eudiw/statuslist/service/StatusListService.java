package no.idporten.eudiw.statuslist.service;

import no.idporten.eudiw.statuslist.domain.StatusEntry;
import no.idporten.eudiw.statuslist.exceptions.StatusListException;
import no.idporten.eudiw.statuslist.exceptions.StatusNotAllocatedException;
import no.idporten.eudiw.statuslist.exceptions.UnsupportedStatusException;
import no.idporten.eudiw.statuslist.issuer.api.StatusEntryUpdateRequest;
import no.idporten.eudiw.statuslist.issuer.config.StatusIssuerProperties;
import no.idporten.eudiw.statuslist.logging.audit.AuditEntryCollection;
import no.idporten.eudiw.statuslist.logging.audit.AuditService;
import no.idporten.eudiw.statuslist.repository.StatusListRepository;
import no.idporten.eudiw.statuslist.repository.models.AllocatedIndexDto;
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

    public String getJsonStatusList(String id) {
        StatusList statusList = statusListRepository.getStatusList(id);
        byte[] packedBytes = packStatuses(statusList);
        byte[] compressed = compressZlib(packedBytes);
        return HexFormat.of().formatHex(compressed);
    }


    public String getStatus(String id, int index) {
        int status = statusListRepository.getStatus(id, index);
        return Status.getStatus(status);
    }

    public List<StatusEntry> allocateToStatusList(int count) {
        List<StatusEntry> statusEntries = new ArrayList<>();

        List<AllocatedIndexDto> allocatedIndexesDto = statusListRepository.allocateToStatusList(count);
        for (AllocatedIndexDto dto : allocatedIndexesDto) {
            URI uri = buildUri(statusIssuerProperties.uri(), dto.listId());
            statusEntries.add(new StatusEntry(dto.index(), uri));
        }

        auditService.logAllocatedEntries(new AuditEntryCollection<>(statusEntries));
        return statusEntries;
    }

    public void revokeStatuses(List<StatusEntryUpdateRequest> entryUpdates) {
        for (StatusEntryUpdateRequest entry : entryUpdates) {
            updateStatus(getListId(entry.uri()), entry.idx(), Status.INVALID);
        }

        auditService.logUpdatedEntries(new AuditEntryCollection<>(entryUpdates), Status.getStatus(Status.INVALID));
    }

    public void updateStatus(String id, int index, int status) {
        if (!Status.isSupported(status)) {
            throw new UnsupportedStatusException(status);
        }

        StatusList statusList = statusListRepository.getStatusList(id);

        if (!statusList.containsIndex(index)) {
            throw new StatusNotAllocatedException(id, index);
        }

        statusList.updateStatus(index, status);
    }

    private byte[] packStatuses(StatusList statusList) {
        int size = statusList.getListSize();

        int bitsPerStatus = statusList.getBitsPerStatus();
        byte[] packed = new byte[(size * bitsPerStatus + 7) / 8];
        int maxStatusValue = (1 << bitsPerStatus) - 1;

        for (int index = 0; index < size; index++) {
            int statusValue = statusList.getStatus(index);
            if (statusValue < 0 || statusValue > maxStatusValue) {
                throw new StatusListException(
                        "Status %d exceeds the allowed size of %d bits at index %d in list with id %s"
                                .formatted(statusValue, bitsPerStatus, index, statusList.getId())
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
