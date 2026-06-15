package one.jpro.platform.playwright;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Helpers for polling and probing a JPro server independently of the {@link JProPlaywrightTest}
 * base class. Useful when a test manages the server lifecycle itself.
 */
public final class JProServerUtils {

    private JProServerUtils() {}

    /**
     * Polls {@code condition} every 10ms with a 4-tier diagnostic budget:
     * <ul>
     *   <li>&lt; 50% of timeout: silent success</li>
     *   <li>50%–100%: success with a WARNING printed</li>
     *   <li>100%–200%: AssertionError — became true, but too late (budget likely too tight)</li>
     *   <li>&gt; 200%: AssertionError — never became true</li>
     * </ul>
     */
    public static void waitFor(long timeout, TimeUnit timeUnit, Supplier<Boolean> condition, Supplier<String> message) {
        long timeoutMs = timeUnit.toMillis(timeout);
        long doubleTimeoutMs = timeoutMs * 2;
        long startMs = System.currentTimeMillis();

        while (true) {
            if (condition.get()) {
                long elapsedMs = System.currentTimeMillis() - startMs;
                if (elapsedMs < timeoutMs / 2) {
                    return;
                } else if (elapsedMs < timeoutMs) {
                    System.out.println("WARNING: waitFor took " + elapsedMs + "ms of " + timeoutMs
                            + "ms timeout. State: " + message.get());
                    return;
                } else {
                    throw new AssertionError("Condition became true after " + elapsedMs
                            + "ms, exceeding timeout of " + timeoutMs + "ms. State: " + message.get());
                }
            }
            long elapsedMs = System.currentTimeMillis() - startMs;
            if (elapsedMs >= doubleTimeoutMs) {
                String state;
                try {
                    state = message.get();
                } catch (Exception e) {
                    state = "(could not evaluate: " + e.getMessage() + ")";
                }
                throw new AssertionError("Condition was never true (waited " + elapsedMs
                        + "ms, 2x timeout of " + timeoutMs + "ms). State: " + state);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Waits up to {@code timeout} for {@code url + "/status/alive"} to answer {@code "alive"}. */
    public static void waitForAlive(String url, long timeout, TimeUnit timeUnit) {
        HttpClient client = HttpClient.newHttpClient();
        String target = url + "/status/alive";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(target)).build();
        waitFor(timeout, timeUnit, () -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return "alive".equals(response.body());
            } catch (IOException | InterruptedException e) {
                return false;
            }
        }, () -> "Server did not become alive (url: " + target + ")");
    }

    /** True iff {@code port} can be bound for listening right now. */
    public static boolean isPortAvailable(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
