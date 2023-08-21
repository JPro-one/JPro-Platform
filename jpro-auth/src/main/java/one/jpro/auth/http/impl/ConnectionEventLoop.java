package one.jpro.auth.http.impl;

import one.jpro.auth.http.HttpOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class represents an independent, threaded event loop for managing a group of connections.
 * It has its own selector, direct off-heap byte buffer, timeout queue, task queue, and state-per-connection.
 * <p>
 * ConnectionEventLoop instances are managed by a parent EventLoop.
 *
 * @author Besmir Beqiri
 */
class ConnectionEventLoop {

    private static final Logger log = LoggerFactory.getLogger(ConnectionEventLoop.class);

    private final HttpOptions options;
    private final Handler handler;
    private final AtomicLong connectionCounter;
    private final AtomicBoolean stop;

    private final Scheduler scheduler;
    private final Queue<Runnable> taskQueue;
    private final ByteBuffer buffer;
    private final Selector selector;
    private final Thread thread;

    /**
     * Constructs a ConnectionEventLoop instance.
     *
     * @param options            the HTTP options to use
     * @param handler            the handler to process requests and responses
     * @param connectionCounter  an atomic counter for generating connection IDs
     * @param stop               an atomic boolean indicating whether to stop the event loop
     * @throws IOException if an I/O error occurs during initialization
     */
    ConnectionEventLoop(HttpOptions options, Handler handler,
                        AtomicLong connectionCounter, AtomicBoolean stop) throws IOException {
        this.options = options;
        this.handler = handler;
        this.connectionCounter = connectionCounter;
        this.stop = stop;

        scheduler = new Scheduler();
        taskQueue = new ConcurrentLinkedQueue<>();
        buffer = ByteBuffer.allocateDirect(options.getReadBufferSize());
        selector = Selector.open();
        thread = new Thread(this::run, "connection-event-loop");
        thread.setDaemon(true);
    }

    /**
     * Represents a single connection in the ConnectionEventLoop.
     */
    private class Connection {

        /**
         * The HTTP version 1.0.
         */
        static final String HTTP_1_0 = "HTTP/1.0";

        /**
         * The HTTP version 1.1.
         */
        static final String HTTP_1_1 = "HTTP/1.1";

        /**
         * The "Connection" header name.
         */
        static final String HEADER_CONNECTION = "Connection";

        /**
         * The "Content-Length" header name.
         */
        static final String HEADER_CONTENT_LENGTH = "Content-Length";

        /**
         * The "Keep-Alive" header value.
         */
        static final String KEEP_ALIVE = "Keep-Alive";

        /**
         * The SocketChannel associated with the connection.
         */
        final SocketChannel socketChannel;

        /**
         * The SelectionKey for the channel.
         */
        final SelectionKey selectionKey;

        /**
         * The ByteTokenizer for processing incoming bytes.
         */
        final ByteTokenizer byteTokenizer;

        /**
         * The unique identifier for the connection.
         */
        final String id;

        /**
         * The RequestParser for parsing incoming requests.
         */
        RequestParser requestParser;

        /**
         * The ByteBuffer for writing responses.
         */
        ByteBuffer writeBuffer;

        /**
         * The task with timeout representing the request.
         */
        Cancellable requestTimeoutTask;

        /**
         * Indicates whether the HTTP version is 1.0.
         */
        boolean httpOneDotZero;

        /**
         * Indicates whether the connection should be kept alive.
         */
        boolean keepAlive;

        /**
         * Constructs a Connection object.
         *
         * @param socketChannel   The SocketChannel associated with the connection.
         * @param selectionKey    The SelectionKey for the channel.
         */
        private Connection(SocketChannel socketChannel, SelectionKey selectionKey) {
            this.socketChannel = socketChannel;
            this.selectionKey = selectionKey;
            byteTokenizer = new ByteTokenizer();
            id = Long.toString(connectionCounter.getAndIncrement());
            requestParser = new RequestParser(byteTokenizer);
            requestTimeoutTask = scheduler.schedule(this::onRequestTimeout, options.getRequestTimeout());
        }

        /**
         * Called when the request times out.
         */
        private void onRequestTimeout() {
            log.trace("Request timeout in connection with id: {}", id);
            failSafeClose();
        }

        /**
         * Called when the socket channel is readable.
         */
        private void onReadable() {
            try {
                doOnReadable();
            } catch (IOException | RuntimeException ex) {
                log.error("Read error in connection with id: {}", id);
                failSafeClose();
            }
        }

        /**
         * Handles the readable event.
         *
         * @throws IOException If an I/O error occurs.
         */
        private void doOnReadable() throws IOException {
            buffer.clear();
            int numBytes = socketChannel.read(buffer);
            if (numBytes < 0) {
                log.trace("Close read in connection with id: {}", id);
                failSafeClose();
                return;
            }
            buffer.flip();
            byteTokenizer.add(buffer);
            log.trace("Read bytes in connection with id: {}, read_bytes: {}, request_bytes: {}",
                    id, numBytes, byteTokenizer.remaining());
            if (requestParser.parse()) {
                log.trace("Read request with connection id: {} and request_bytes: {}", id, byteTokenizer.remaining());
                onParseRequest();
            } else {
                if (byteTokenizer.size() > options.getMaxRequestSize()) {
                    log.trace("Exceed request max_size in connection with id: {} and request_size: {}", id, byteTokenizer.size());
                    failSafeClose();
                }
            }
        }

        /**
         * Handles the parsed request.
         */
        private void onParseRequest() {
            if (selectionKey.interestOps() != 0) {
                selectionKey.interestOps(0);
            }
            if (requestTimeoutTask != null) {
                requestTimeoutTask.cancel();
                requestTimeoutTask = null;
            }
            Request request = requestParser.request();
            httpOneDotZero = request.getVersion().equalsIgnoreCase(HTTP_1_0);
            keepAlive = request.hasHeader(HEADER_CONNECTION, KEEP_ALIVE);
            byteTokenizer.compact();
            requestParser = new RequestParser(byteTokenizer);
            handler.handle(request, this::onResponse);
        }

        /**
         * Handles the response from the handler.
         *
         * @param response The response to be sent.
         */
        private void onResponse(Response response) {
            // Enqueue the callback invocation and wake the selector
            // to ensure proper handling when invoked from the event loop thread
            taskQueue.add(() -> {
                try {
                    prepareToWriteResponse(response);
                } catch (IOException ex) {
                    log.trace("Response error in connection with id: {}", id);
                    failSafeClose();
                }
            });
            // Wake up the selector if the callback was invoked from a different thread
            if (Thread.currentThread() != thread) {
                selector.wakeup();
            }
        }

        /**
         * Prepares to write the response.
         *
         * @param response The response to be written.
         * @throws IOException If an I/O error occurs.
         */
        private void prepareToWriteResponse(Response response) throws IOException {
            String version = httpOneDotZero ? HTTP_1_0 : HTTP_1_1;
            List<Header> headers = new ArrayList<>();
            if (httpOneDotZero && keepAlive) {
                headers.add(new Header(HEADER_CONNECTION, KEEP_ALIVE));
            }
            if (!response.hasHeader(HEADER_CONTENT_LENGTH)) {
                headers.add(new Header(HEADER_CONTENT_LENGTH, Integer.toString(response.getBody().length)));
            }
            writeBuffer = ByteBuffer.wrap(response.serialize(version, headers));
            log.trace("Response ready in connection with id: {} and num_bytes: {}", id, writeBuffer.remaining());
            doOnWritable();
        }

        /**
         * Called when the socket channel is writable.
         */
        private void onWritable() {
            try {
                doOnWritable();
            } catch (IOException | RuntimeException ex) {
                log.trace("Write error in connection with id: {}", id);
                failSafeClose();
            }
        }

        /**
         * Writes data to the socket channel.
         *
         * @return The number of bytes written.
         * @throws IOException If an I/O error occurs.
         */
        private int doWrite() throws IOException {
            buffer.clear(); // pos = 0, limit = capacity
            int amount = Math.min(buffer.remaining(), writeBuffer.remaining()); // determine transfer quantity
            buffer.put(writeBuffer.array(), writeBuffer.position(), amount); // do transfer
            buffer.flip();
            int written = socketChannel.write(buffer);
            writeBuffer.position(writeBuffer.position() + written); // advance write buffer
            return written;
        }

        /**
         * Handles the writable event.
         *
         * @throws IOException If an I/O error occurs.
         */
        private void doOnWritable() throws IOException {
            int numBytes = doWrite();
            if (!writeBuffer.hasRemaining()) { // Response fully written
                writeBuffer = null; // done with current write buffer, remove reference
                log.trace("Write response with connection id: {} and num_bytes: {}", id, numBytes);
                if (httpOneDotZero && !keepAlive) { // non-persistent connection, close now
                    log.trace("Close after response with connection id: {}", id);
                    failSafeClose();
                } else { // Persistent connection
                    if (requestParser.parse()) { // Subsequent request in the buffer
                        log.trace("Pipeline request with connection id: {} and request_bytes: {}", id, byteTokenizer.remaining());
                        onParseRequest();
                    } else { // Switch back to read mode
                        requestTimeoutTask = scheduler.schedule(this::onRequestTimeout, options.getRequestTimeout());
                        selectionKey.interestOps(SelectionKey.OP_READ);
                    }
                }
            } else { // Response not fully written, remain in write mode
                if ((selectionKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                }
                log.trace("Write in connection with id: {} and num_bytes: {}", id, numBytes);
            }
        }

        /**
         * Closes the connection safely.
         */
        private void failSafeClose() {
            try {
                if (requestTimeoutTask != null) {
                    requestTimeoutTask.cancel();
                }
                selectionKey.cancel();
                socketChannel.close();
            } catch (IOException e) {
                // suppress error
            }
        }
    }

    /**
     * Returns the number of active connections.
     *
     * @return The number of active connections.
     */
    int numConnections() {
        return selector.keys().size();
    }

    /**
     * Starts the server.
     */
    void start() {
        thread.start();
    }

    /**
     * Blocks the current thread until the server thread terminates.
     *
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    void join() throws InterruptedException {
        thread.join();
    }

    /**
     * Runs the server loop.
     */
    private void run() {
        try {
            doStart();
        } catch (IOException e) {
            stop.set(true); // stop the world on critical error
        }
    }

    /**
     * Starts the server loop.
     *
     * @throws IOException If an I/O error occurs.
     */
    private void doStart() throws IOException {
        while (!stop.get()) {
            selector.select(options.getResolution().toMillis());
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();
            while (it.hasNext()) {
                SelectionKey selKey = it.next();
                if (selKey.isReadable()) {
                    ((Connection) selKey.attachment()).onReadable();
                } else if (selKey.isWritable()) {
                    ((Connection) selKey.attachment()).onWritable();
                }
                it.remove();
            }
            scheduler.expired().forEach(Runnable::run);
            Runnable task;
            while ((task = taskQueue.poll()) != null) {
                task.run();
            }
        }
    }

    /**
     * Registers a new socket channel with the server.
     *
     * @param socketChannel The socket channel to register.
     */
    void register(SocketChannel socketChannel) {
        taskQueue.add(() -> {
            try {
                doRegister(socketChannel);
            } catch (IOException ex) {
                log.error("Error on registering a new socket channel", ex);
                try {
                    socketChannel.close();
                } catch (IOException ignore) {}
            }
        });
        selector.wakeup(); // wakeup event loop thread to process a task immediately
    }

    /**
     * Registers a socket channel with the selector and attaches a Connection instance to it.
     *
     * @param socketChannel The socket channel to register.
     * @throws IOException If an I/O error occurs.
     */
    private void doRegister(SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);
        SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        Connection connection = new Connection(socketChannel, selectionKey);
        selectionKey.attach(connection);
    }
}
