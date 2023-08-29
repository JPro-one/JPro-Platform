package one.jpro.platform.imagemanager;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class Utils {

    public static long computeHashValue(byte[] data) {

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);

            // Using ByteBuffer to transform the first 8 bytes of the hash into a long value.
            return ByteBuffer.wrap(digest, 0, 8).getLong();
        } catch (Exception e) {
            throw new RuntimeException("Error computing hash value", e);
        }
    }

    public static String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
