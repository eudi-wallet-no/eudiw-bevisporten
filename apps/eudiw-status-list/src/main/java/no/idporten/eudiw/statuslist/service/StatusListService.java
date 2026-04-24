package no.idporten.eudiw.statuslist.service;

import no.idporten.eudiw.statuslist.logging.audit.AuditService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Deflater;

import static no.idporten.eudiw.statuslist.service.Status.VALID;

@Service
public class StatusListService {

    private final AuditService auditService;

    private final static int listSize = 1_000_000;

    private Map<Integer, Integer> statuslist;
    private final IntStack freeIndexStack = createFreeIndexStack();
    private int bitsPerStatus = 1;

    public StatusListService(AuditService auditService) {
        this.auditService = auditService;
        mockEmptyList();
    }

    public void setStatuslist(Map<Integer, Integer> statuslist) {
        this.statuslist = statuslist;
    }

    public void setBitsPerStatus(int bitsPerStatus) {
        if (bitsPerStatus < 1 || bitsPerStatus > 8) {
            throw new IllegalArgumentException("bitsPerStatus must be between 1 and 8");
        }
        this.bitsPerStatus = bitsPerStatus;
    }

    // berre midlertidig metode, skal bort.
    private void mockEmptyList() {
        // Mock default list with all valid
        statuslist = IntStream.range(0, listSize)
                .boxed()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> VALID
                ));
    }

    public Map<Integer, Integer> getStatuslist() {
        return statuslist;
    }

    public String getJsonStatuslist() {
        byte[] packedBytes = packStatuses();
        byte[] compressed = compressZlib(packedBytes);
        return HexFormat.of().formatHex(compressed);
    }

    private byte[] packStatuses() {
        int size = statuslist.keySet().stream()
                .max(Integer::compareTo)
                .map(maxIndex -> maxIndex + 1)
                .orElse(0);

        byte[] packed = new byte[(size * bitsPerStatus + 7) / 8];
        int maxStatusValue = (1 << bitsPerStatus) - 1;

        for (int index = 0; index < size; index++) {
            int statusValue = Optional.ofNullable(statuslist.get(index)).orElse(VALID);
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

    public static byte[] compressZlib(byte[] input) {
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

    public List<Integer> allocateToStatusList(int count) {
        if (freeIndexStack.size() < count) {
            throw new RuntimeException("Not enough space in statuslist to allocate " + count + " entries.");
        }
        List<Integer> allocatedIndexes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            allocatedIndexes.add(freeIndexStack.pop());
        }
        auditService.logAllocateIndexes(allocatedIndexes);
        return allocatedIndexes;
    }

    public String getStatus(int index) {
        if (!statuslist.containsKey(index)) {
            throw new IllegalArgumentException("Index " + index + " does not exist in statuslist");
        }
        int status = Optional.ofNullable(statuslist.get(index)).orElse(VALID);
        return Status.getStatus(status);
    }

    public void revokeStatuses(List<Integer> indexes) {
        int revokeStatus = Status.INVALID;
        for (int index : indexes) {
            updateStatus(index, revokeStatus);
        }
        auditService.logUpdateIndexes(indexes, Status.getStatus(revokeStatus));
    }


    protected void updateStatus(int index, int status) {
        if(!List.of(Status.VALID, Status.INVALID).contains(status)){
            throw new IllegalArgumentException("Invalid status value: " + status);
        }
        if(!statuslist.containsKey(index)){
            throw new IllegalArgumentException("Index " + index + " does not exist in statuslist");
        }
        statuslist.put(index, status);
    }

    private IntStack createFreeIndexStack() {
        int[] numbers = new int[listSize];

        for (int i = 0; i < listSize; i++) {
            numbers[i] = i;
        }

        Random random = new Random();
        for (int i = listSize - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = numbers[i];
            numbers[i] = numbers[j];
            numbers[j] = temp;
        }

        return new IntStack(numbers, listSize);
    }
}
