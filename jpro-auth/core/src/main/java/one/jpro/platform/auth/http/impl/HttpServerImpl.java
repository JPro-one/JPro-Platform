package one.jpro.platform.auth.http.impl;

import javafx.application.Platform;
import javafx.stage.Stage;
import one.jpro.platform.auth.http.HttpOptions;
import one.jpro.platform.auth.http.HttpServer;
import one.jpro.platform.auth.http.HttpServerException;
import one.jpro.platform.auth.http.HttpStatus;
import one.jpro.platform.internal.openlink.OpenLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of the {@link HttpServer} interface when running
 * the application in a desktop/device environment.
 *
 * @author Besmir Beqiri
 */
public final class HttpServerImpl implements HttpServer {

    private static final Logger log = LoggerFactory.getLogger(HttpServerImpl.class);

    /**
     * Common header for the MIME Content-Type
     */
    static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * Common MIME type for dynamic content: plain text
     */
    static final String MIME_PLAINTEXT = "text/plain";
    static final String MIME_HTML = "text/html";
    static final byte[] SPACE = " ".getBytes();
    static final byte[] CRLF = "\r\n".getBytes();

    private String uri;

    @Nullable
    private final Stage stage;
    @NotNull
    private final HttpOptions options;
    private final Selector selector;
    private final AtomicBoolean stop;
    private final ServerSocketChannel serverSocketChannel;
    private final List<ConnectionEventLoop> connectionEventLoops;
    private final CompletableFuture<String> serverResponseFuture = new CompletableFuture<>();
    private final Thread thread;

    /**
     * Creates HTTP server.
     *
     * @param options the HTTP options
     * @throws IOException if an error occurs
     */
    public HttpServerImpl(@Nullable final Stage stage, @NotNull final HttpOptions options) throws IOException {
        this.stage = stage;
        this.options = Objects.requireNonNull(options, "Http options cannot be null");

        // Create a default response
        final Response response = new Response(
                HttpStatus.OK.getCode(),
                HttpStatus.OK.getMessage(),
                List.of(new Header(HEADER_CONTENT_TYPE, MIME_HTML)),
                getResourceAsBytes("default-response.html"));

        final Handler handler = (request, callback) -> {
            this.uri = request.uri();

            log.debug("***************************************************************************");
            log.debug("Server host: {}", getServerHost());
            log.debug("Server port: {}", getServerPort());
            log.debug("Full requested URL: {}", getFullRequestedURL());
            log.debug("Parameters: {}", getParameters());
            log.debug("Request URI: {}", request.uri());
            log.debug("Request method: {}", request.method());
            log.debug("Request version: {}", request.version());
            log.debug("Request headers: {}", request.headers());
            log.debug("Response status: {}", response.status());
            log.debug("Response body: {}", new String(response.body()));
            log.debug("***************************************************************************");

            callback.accept(response);
            serverResponseFuture.complete(uri);
        };

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        selector = Selector.open();
        stop = new AtomicBoolean();

        AtomicLong connectionCounter = new AtomicLong();
        connectionEventLoops = new ArrayList<>();
        for (int i = 0; i < options.getConcurrency(); i++) {
            connectionEventLoops.add(new ConnectionEventLoop(options, handler, connectionCounter, stop));
        }

        thread = new Thread(this::run, "http-server-thread");
        thread.setDaemon(true);

        InetSocketAddress address = options.getHost() == null
                ? new InetSocketAddress(options.getPort()) // wildcard address
                : new InetSocketAddress(options.getHost(), options.getPort());

        serverSocketChannel = ServerSocketChannel.open();
        if (options.isReuseAddr()) {
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, options.isReuseAddr());
        }
        if (options.isReusePort()) {
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEPORT, options.isReusePort());
        }
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(address, options.getAcceptLength());
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private byte[] getResourceAsBytes(@NotNull final String name) throws IOException {
        try (InputStream is = HttpServer.class.getResourceAsStream(name)) {
            if (is != null) {
                return is.readAllBytes();
            }
        }
        return SPACE;
    }

    @Override
    public void start() {
        thread.start();
        connectionEventLoops.forEach(ConnectionEventLoop::start);
        log.info("Starting server on port: {}", getServerPort());
    }

    private void run() {
        try {
            doRun();
        } catch (IOException ex) {
            log.error("Error on connection termination", ex);
            stop.set(true); // stop the world on critical error
        }
    }

    private void doRun() throws IOException {
        while (!stop.get()) {
            selector.select(options.getResolution().toMillis());
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();
            while (it.hasNext()) {
                SelectionKey selKey = it.next();
                Optional<ConnectionEventLoop> leastConnections = leastConnections();
                if (selKey.isAcceptable() && leastConnections.isPresent()) {
                    ConnectionEventLoop connectionEventLoop = leastConnections.get();
                    connectionEventLoop.register(serverSocketChannel.accept());
                }
                it.remove();
            }
        }
    }

    private Optional<ConnectionEventLoop> leastConnections() {
        return connectionEventLoops.stream().min(Comparator.comparing(ConnectionEventLoop::numConnections));
    }

    @Override
    public void stop() {
        stop.set(true);

        if (serverSocketChannel.isOpen()) {
            try {
                serverSocketChannel.close();
                TimeUnit.MILLISECONDS.sleep(options.getResolution().toMillis());
            } catch (IOException | InterruptedException ex) {
                throw new HttpServerException(ex);
            }
            log.info("Server stopped on port: {}", getServerPort());
        }

        if (selector.isOpen()) {
            try {
                for (SelectionKey key : selector.keys()) {
                    final SelectableChannel channel = key.channel();
                    if (channel.isOpen()) {
                        channel.close();
                    }
                }
                selector.close();
            } catch (IOException ex) {
                throw new HttpServerException(ex);
            }
        }
    }

    @Override
    public String getServerHost() {
        return options.getHost();
    }

    @Override
    public int getServerPort() {
        return options.getPort();
    }

    @Override
    public String getFullRequestedURL() {
        return uri;
    }

    @Override
    public CompletableFuture<String> openURL(@NotNull final String url) {
        return CompletableFuture.runAsync(this::start)
                .thenRun(() -> OpenLink.openURL(URI.create(url).toString()))
                .thenCombine(serverResponseFuture, (result1, result2) -> result2)
                .thenApply(result -> {
                    if (stage != null && stage.isShowing()) {
                        Platform.runLater(stage::toFront);
                    }
                    stop();
                    return result;
                });
    }
}
