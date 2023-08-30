package one.jpro.platform.image;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * This class provides utility methods for various image operations.
 */
public class Utils {

    /**
     * Computes the MD5 hash of the given data and returns the first 8 bytes
     * of the hash as a long value.
     *
     * @param data The input data for which the hash needs to be computed.
     * @return The first 8 bytes of the MD5 hash as a long value.
     * @throws RuntimeException if there's an error computing the hash.
     */
    public static long computeHashValue(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);

            // Using ByteBuffer to transform the first 8 bytes of the hash into a long value.
            return ByteBuffer.wrap(digest, 0, 8).getLong();
        } catch (Exception ex) {
            throw new RuntimeException("Error computing hash value", ex);
        }
    }

    /**
     * Escapes certain special characters in a JSON string to ensure it's valid.
     * Specifically, it escapes backslashes and double quotes.
     *
     * @param str The input JSON string that might contain characters to be escaped.
     * @return The input string with necessary characters escaped.
     */
    public static String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
