package no.idporten.eudiw.verifier.crypto;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;

/**
 * Calls Kotlin suspend functions (used by the walt.id libraries) from blocking Java code.
 */
public final class SuspendBridge {

    private SuspendBridge() {
    }

    /**
     * @param block lambda forwarding the continuation to a suspend function, e.g.
     *              {@code (scope, continuation) -> SomeKt.suspendFunction(arg, continuation)}
     */
    public static <T> T runBlocking(Function2<CoroutineScope, Continuation<? super T>, Object> block) {
        try {
            return BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, block);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for suspend function", e);
        }
    }
}
