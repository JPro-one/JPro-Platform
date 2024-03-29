package one.jpro.platform.auth.core.http.impl;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * HTTP request handler.
 *
 * @author Besmir Beqiri
 */
@FunctionalInterface
interface Handler {

    /**
     * HTTP request handle.
     * This method is called on the event loop thread. It must be non-blocking!
     * The callee must invoke the callback once and only once.
     * The callback may either be invoked synchronously before handle terminates or
     * asynchronously in a background thread.
     * The provided callback object has a reference to internal connection state.
     */
    void handle(@NotNull Request request, @NotNull Consumer<Response> callback);
}
