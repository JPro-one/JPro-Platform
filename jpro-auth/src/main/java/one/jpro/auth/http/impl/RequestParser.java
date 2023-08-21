package one.jpro.auth.http.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * This class is responsible for parsing HTTP requests.
 * It parses the request line, headers, and optional request body.
 * It supports both fixed-length and chunked transfer encoding.
 *
 * @author Besmir Beqiri
 */
final class RequestParser {

    /**
     * CRLF: A byte array representing the carriage return and line feed characters ("\r\n").
     */
    private static final byte[] CRLF = "\r\n".getBytes();

    /**
     * SPACE: A byte array representing a space character (" ").
     */
    private static final byte[] SPACE = " ".getBytes();

    /**
     * HEADER_CONTENT_LENGTH: A string constant for the "Content-Length" header.
     */
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";

    /**
     * HEADER_TRANSFER_ENCODING: A string constant for the "Transfer-Encoding" header.
     */
    private static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";

    /**
     * CHUNKED: A string constant for the "chunked" transfer encoding.
     */
    private static final String CHUNKED = "chunked";

    /**
     * RADIX_HEX: An integer constant representing the hexadecimal radix.
     */
    private static final int RADIX_HEX = 16;

    /**
     * Enumeration representing the different parsing states of the request.
     */
    enum State {
        METHOD(p -> p.tokenizer.next(SPACE), RequestParser::parseMethod),
        URI(p -> p.tokenizer.next(SPACE), RequestParser::parseUri),
        VERSION(p -> p.tokenizer.next(CRLF), RequestParser::parseVersion),
        HEADER(p -> p.tokenizer.next(CRLF), RequestParser::parseHeader),
        BODY(p -> p.tokenizer.next(p.contentLength), RequestParser::parseBody),
        CHUNK_SIZE(p -> p.tokenizer.next(CRLF), RequestParser::parseChunkSize),
        CHUNK_DATA(p -> p.tokenizer.next(p.chunkSize), RequestParser::parseChunkData),
        CHUNK_DATA_END(p -> p.tokenizer.next(CRLF), (rp, token) -> rp.parseChunkDateEnd()),
        CHUNK_TRAILER(p -> p.tokenizer.next(CRLF), (rp, token) -> rp.parseChunkTrailer()),
        DONE(null, null);

        final Function<RequestParser, byte[]> tokenSupplier;
        final BiConsumer<RequestParser, byte[]> tokenConsumer;

        /**
         * Constructs a State enum with the specified token supplier and token consumer.
         *
         * @param tokenSupplier  The function that supplies the next token during parsing.
         * @param tokenConsumer The function that consumes the token during parsing.
         */
        State(Function<RequestParser, byte[]> tokenSupplier, BiConsumer<RequestParser, byte[]> tokenConsumer) {
            this.tokenSupplier = tokenSupplier;
            this.tokenConsumer = tokenConsumer;
        }
    }

    private final ByteTokenizer tokenizer;
    private State state = State.METHOD;
    private int contentLength;
    private int chunkSize;
    private final ByteMerger chunks = new ByteMerger();
    private String method;
    private String uri;
    private String version;
    private final List<Header> headers = new ArrayList<>();
    private byte[] body;

    /**
     * Constructor for RequestParser.
     *
     * @param tokenizer The tokenizer used for tokenizing the request data.
     */
    RequestParser(ByteTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * Parses the HTTP request.
     *
     * @return <code>true</code> if the parsing is successful, <code>false</code> otherwise.
     */
    boolean parse() {
        while (state != State.DONE) {
            byte[] token = state.tokenSupplier.apply(this);
            if (token == null) {
                return false;
            }
            state.tokenConsumer.accept(this, token);
        }
        return true;
    }

    /**
     * Returns the parsed Request object representing the HTTP request.
     *
     * @return The parsed Request object.
     */
    Request request() {
        return new Request(method, uri, version, headers, body);
    }

    /**
     * Parses the method token and updates the state accordingly.
     */
    private void parseMethod(byte[] token) {
        method = new String(token);
        state = State.URI;
    }

    /**
     * Parses the uri token and updates the state accordingly.
     */
    private void parseUri(byte[] token) {
        uri = new String(token);
        state = State.VERSION;
    }

    /**
     * Parses the version token and updates the state accordingly.
     */
    private void parseVersion(byte[] token) {
        version = new String(token);
        state = State.HEADER;
    }

    /**
     * Parses the header token and updates the state accordingly.
     */
    private void parseHeader(byte[] token) {
        if (token.length == 0) { // CR-LF on own line, end of headers
            if (hasMultipleTransferLengths()) {
                throw new IllegalStateException("multiple message lengths");
            }
            Integer contentLength = findContentLength();
            if (contentLength == null) {
                if (hasChunkedEncodingHeader()) {
                    state = State.CHUNK_SIZE;
                } else {
                    state = State.DONE;
                }
            } else {
                this.contentLength = contentLength;
                state = State.BODY;
            }
        } else {
            headers.add(parseHeaderLine(token));
        }
    }

    /**
     * Parses a header line and returns a Header object.
     */
    private static Header parseHeaderLine(byte[] line) {
        int colonIndex = indexOfColon(line);
        if (colonIndex <= 0) {
            throw new IllegalStateException("malformed header line");
        }
        int spaceIndex = colonIndex + 1;
        while (spaceIndex < line.length && line[spaceIndex] == ' ') { // advance beyond variable-length space prefix
            spaceIndex++;
        }
        return new Header(
                new String(line, 0, colonIndex),
                new String(line, spaceIndex, line.length - spaceIndex));
    }

    /**
     * Finds the index of the colon character in a byte array.
     */
    private static int indexOfColon(byte[] line) {
        for (int i = 0; i < line.length; i++) {
            if (line[i] == ':') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Parses the chunk size token and updates the state accordingly.
     */
    private void parseChunkSize(byte[] token) {
        try {
            chunkSize = Integer.parseInt(new String(token), RADIX_HEX);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("invalid chunk size");
        }
        state = chunkSize == 0
                ? State.CHUNK_TRAILER
                : State.CHUNK_DATA;
    }

    /**
     * Parses the chunk data token and updates the state accordingly.
     */
    private void parseChunkData(byte[] token) {
        chunks.add(token);
        state = State.CHUNK_DATA_END;
    }

    /**
     * Parses the chunk data end token and updates the state accordingly.
     */
    private void parseChunkDateEnd() {
        state = State.CHUNK_SIZE;
    }

    /**
     * Parses the chunk trailer token and updates the state accordingly.
     */
    private void parseChunkTrailer() {
        body = chunks.merge();
        state = State.DONE;
    }

    /**
     * Parses the body token and updates the state accordingly.
     */
    private void parseBody(byte[] token) {
        body = token;
        state = State.DONE;
    }

    /**
     * Checks if there are multiple transfer length headers in the request.
     */
    private boolean hasMultipleTransferLengths() {
        int count = 0;
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(HEADER_CONTENT_LENGTH)
                    || header.getName().equalsIgnoreCase(HEADER_TRANSFER_ENCODING)) {
                count++;
            }
        }
        return count > 1;
    }

    /**
     * Finds the content length from the request headers.
     */
    private Integer findContentLength() {
        try {
            for (Header header : headers) {
                if (header.getName().equalsIgnoreCase(HEADER_CONTENT_LENGTH)) {
                    return Integer.parseInt(header.getValue());
                }
            }
            return null;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("invalid content-length header value");
        }
    }

    /**
     * Checks if the request has a chunked encoding header.
     */
    private boolean hasChunkedEncodingHeader() {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(HEADER_TRANSFER_ENCODING)
                    && header.getValue().equalsIgnoreCase(CHUNKED)) {
                return true;
            }
        }
        return false;
    }
}
