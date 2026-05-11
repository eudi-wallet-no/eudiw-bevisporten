package no.idporten.eudiw.statuslist.util;

import no.idporten.eudiw.statuslist.service.IntStack;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class StatusListUtil {
    public static String getListId(URI uri) {
        String[] parts = uri.getPath().split("/");
        return parts[parts.length - 1];
    }

    public static URI buildUri(String baseUri, String listId) {
        return UriComponentsBuilder.fromUriString(baseUri).buildAndExpand(listId).toUri();
    }

    public static IntStack createFreeIndexStack(int count, int seed) {
        return createFreeIndexStack(count, seed, count-1);
    }

    public static IntStack createFreeIndexStack(int count, int seed, int next) {
        int[] numbers = new int[count];

        for (int i = 0; i < count; i++) {
            numbers[i] = i;
        }

        SeededRandom random = new SeededRandom(seed);
        for (int i = count - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = numbers[i];
            numbers[i] = numbers[j];
            numbers[j] = temp;
        }

        return new IntStack(numbers, next);
    }
}
