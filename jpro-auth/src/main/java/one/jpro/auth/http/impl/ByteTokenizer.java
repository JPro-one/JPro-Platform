package one.jpro.auth.http.impl;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * ByteTokenizer is an expandable, first-in first-out byte array that supports tokenization.
 * Bytes are added at the tail and tokenization occurs at the head.
 *
 * @author Besmir Beqiri
 */
final class ByteTokenizer {

    private byte[] array = new byte[0];
    private int position;
    private int size;

    /**
     * Returns the current size of the tokenized byte array.
     *
     * @return the size of the byte array
     */
    int size() {
        return size;
    }

    /**
     * Returns the current capacity of the underlying byte array.
     *
     * @return the capacity of the byte array
     */
    int capacity() {
        return array.length;
    }

    /**
     * Returns the number of remaining bytes to be tokenized.
     *
     * @return the number of remaining bytes
     */
    int remaining() {
        return size - position;
    }

    /**
     * Compacts the underlying byte array by removing processed bytes
     * and shifting the remaining bytes to the beginning.
     * After compaction, the position is reset to 0.
     */
    void compact() {
        array = Arrays.copyOfRange(array, position, size);
        size = size - position;
        position = 0;
    }

    /**
     * Adds bytes from a ByteBuffer to the tokenized byte array.
     * The added bytes are appended to the end of the existing byte array.
     *
     * @param buffer the ByteBuffer containing the bytes to add
     */
    void add(ByteBuffer buffer) {
        int bufferLen = buffer.remaining();
        if (array.length - size < bufferLen) {
            array = Arrays.copyOf(array, Math.max(size + bufferLen, array.length * 2));
        }
        buffer.get(array, size, bufferLen);
        size += bufferLen;
    }

    /**
     * Retrieves the next fixed-length chunk of bytes from the tokenized byte array.
     * The position is advanced by the specified length.
     *
     * @param length the length of the chunk to retrieve
     * @return the next chunk of bytes, or null if there are not enough bytes remaining
     */
    byte[] next(int length) {
        if (size - position < length) {
            return null;
        }
        byte[] result = Arrays.copyOfRange(array, position, position + length);
        position += length;
        return result;
    }

    /**
     * Retrieves the next chunk of bytes from the tokenized byte array, delimited by the specified byte array.
     * The position is advanced to the end of the delimiter.
     *
     * @param delimiter the byte array used as the delimiter
     * @return the next chunk of bytes, or null if the delimiter is not found
     */
    byte[] next(byte[] delimiter) {
        int index = indexOf(delimiter);
        if (index < 0) {
            return null;
        }
        byte[] result = Arrays.copyOfRange(array, position, index);
        position = index + delimiter.length;
        return result;
    }

    /**
     * Searches for the index of the specified byte array within the tokenized byte array,
     * starting from the current position.
     *
     * @param delimiter the byte array to search for
     * @return the index of the delimiter, or -1 if it is not found
     */
    private int indexOf(byte[] delimiter) {
        for (int i = position; i <= size - delimiter.length; i++) {
            if (Arrays.equals(delimiter, 0, delimiter.length, array, i, i + delimiter.length)) {
                return i;
            }
        }
        return -1;
    }
}
