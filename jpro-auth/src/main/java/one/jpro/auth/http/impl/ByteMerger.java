package one.jpro.auth.http.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * ByteMerger is a utility that accumulates a sequence of byte arrays
 * and merges them into a single flat array upon request.
 *
 * @author Besmir Beqiri
 */
final class ByteMerger {

    private final List<byte[]> arrays = new ArrayList<>();

    /**
     * Adds a byte array to the merger.
     *
     * @param array the byte array to add
     */
    void add(byte[] array) {
        arrays.add(array);
    }

    /**
     * Merges all the added byte arrays into a single byte array.
     *
     * @return the merged byte array
     */
    byte[] merge() {
        int size = sumOfLengths();
        byte[] result = new byte[size];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    /**
     * Calculates the sum of lengths of all the added byte arrays.
     *
     * @return the sum of lengths
     */
    int sumOfLengths() {
        int sum = 0;
        for (byte[] array : arrays) {
            sum += array.length;
        }
        return sum;
    }
}
