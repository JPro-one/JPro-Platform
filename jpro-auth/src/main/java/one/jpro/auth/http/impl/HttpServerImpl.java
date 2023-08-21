package one.jpro.auth.http.impl;

import javafx.application.Platform;
import javafx.stage.Stage;
import one.jpro.auth.http.HttpServer;
import one.jpro.auth.http.HttpServerException;
import one.jpro.auth.http.HttpOptions;
import one.jpro.auth.http.HttpStatus;
import one.jpro.auth.utils.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.*;
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

    private static Stage stage;
    private static HttpOptions httpOptions;

    private static final class SingletonHolder {
        private static final HttpServerImpl INSTANCE = initAuthServer();

        private static HttpServerImpl initAuthServer() {
            try {
                return new HttpServerImpl(stage, httpOptions == null ? new HttpOptions() : httpOptions);
            } catch (IOException ex) {
                throw new HttpServerException(ex.getMessage(), ex);
            }
        }

        /**
         * Prevent instantiation.
         */
        private SingletonHolder() {
        }

        /**
         * Get singleton AuthServer.
         *
         * @return AuthServer singleton.
         */
        public static HttpServerImpl getInstance() {
            return SingletonHolder.INSTANCE;
        }
    }

    public static HttpServerImpl getInstance(Stage stage) throws HttpServerException {
        HttpServerImpl.stage = stage;
        return SingletonHolder.getInstance();
    }

    public static HttpServerImpl getInstance(Stage stage, HttpOptions httpOptions) throws HttpServerException {
        HttpServerImpl.stage = stage;
        HttpServerImpl.httpOptions = httpOptions;
        return SingletonHolder.getInstance();
    }

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

    private final HttpOptions options;
    private final Selector selector;
    private final AtomicBoolean stop;
    private final ServerSocketChannel serverSocketChannel;
    private final List<ConnectionEventLoop> connectionEventLoops;
    private final Thread thread;

    /**
     * Creates authorization server.
     *
     * @param options the HTTP options
     * @throws IOException if an error occurs
     */
    private HttpServerImpl(Stage stage, HttpOptions options) throws IOException {
        this.options = options;

        // Create a default response
        final Response response = new Response(
                HttpStatus.OK.getCode(),
                HttpStatus.OK.getMessage(),
                List.of(new Header(HEADER_CONTENT_TYPE, MIME_HTML)),
                getResourceAsBytes("default-response.html"));

        final Handler handler = (request, callback) -> {
            this.uri = request.getUri();

            log.debug("***************************************************************************");
            log.debug("Server host: {}", getServerHost());
            log.debug("Server port: {}", getServerPort());
            log.debug("Full requested URL: {}", getFullRequestedURL());
            log.debug("Parameters: {}", getParameters());
            log.debug("Request hashCode = {}", HttpServerImpl.this.hashCode());
            log.debug("Request URI: {}", request.getUri());
            log.debug("Request method: {}", request.getMethod());
            log.debug("Request version: {}", request.getVersion());
            log.debug("Request headers: {}", request.getHeaders());
            log.debug("Request body: {}", new String(request.getBody()));
            log.debug("Response status: {}", response.getStatus());
            log.debug("***************************************************************************");

            callback.accept(response);

            if (stage != null && stage.isShowing()) {
                Platform.runLater(() -> {
                    this.uri = request.getUri();
                    // TODO: trigger the browser to open the URL, or add handler to the response
                    stage.toFront();
                });
            }
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

        // start the server
        start();
    }

    private byte[] getResourceAsBytes(@NotNull String name) throws IOException {
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
        log.info("Stopping the server");
    }

    @Override
    public String getServerHost() {
        return options.getHost();
    }

    @Override
    public int getServerPort() {
        int port = -1;
        try {
            final SocketAddress localAddress = serverSocketChannel.getLocalAddress();
            if (localAddress instanceof InetSocketAddress) {
                final InetSocketAddress socketAddress = (InetSocketAddress) serverSocketChannel.getLocalAddress();
                port = socketAddress.getPort();
            }
        } catch (IOException ex) {
            throw new HttpServerException(ex.getMessage(), ex);
        }
        return port;
    }

    @Override
    public String getFullRequestedURL() {
        return uri;
    }

    @Override
    public void openURL(@NotNull URI uri) {
        try {
            if (PlatformUtils.isMac()) {
                Runtime.getRuntime().exec("open " + uri);
            } else if (PlatformUtils.isWindows()) {
                Runtime.getRuntime().exec("start \"" + uri + "\"");
            } else if (PlatformUtils.isLinux()) {
                Runtime.getRuntime().exec("xdg-open " + uri);
            }
        } catch (IOException ex) {
            log.error("Unable to open the browser!", ex);
        }
    }
}
