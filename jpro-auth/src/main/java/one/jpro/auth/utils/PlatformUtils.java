package one.jpro.auth.utils;

/**
 * Platform utilities.
 *
 * @author Besmir Beqiri
 */
public class PlatformUtils {

    private static final String os = System.getProperty("os.name");
    private static final String version = System.getProperty("os.version");
    private static final String javafxPlatform = System.getProperty("javafx.platform");
    private static final boolean embedded = Boolean.getBoolean("com.sun.javafx.isEmbedded");
    private static final boolean ANDROID = "android".equals(javafxPlatform) || "Dalvik".equals(System.getProperty("java.vm.name"));
    private static final boolean WINDOWS = os.startsWith("Windows");
    private static final boolean MAC = os.startsWith("Mac");
    private static final boolean LINUX = os.startsWith("Linux") && !ANDROID;
    private static final boolean IOS = os.startsWith("iOS");


    /**
     * Returns true if the operating system is a form of Windows.
     */
    public static boolean isWindows(){
        return WINDOWS;
    }

    /**
     * Returns true if the operating system is a form of macOS.
     */
    public static boolean isMac(){
        return MAC;
    }

    /**
     * Returns true if the operating system is a form of Linux.
     */
    public static boolean isLinux() {
        return LINUX;
    }

    /**
     * Returns true if the platform is embedded.
     */
    public static boolean isEmbedded() {
        return embedded;
    }

    public static boolean isAndroid() {
        return ANDROID;
    }

    /**
     * Returns true if the operating system is iOS
     */
    public static boolean isIOS(){
        return IOS;
    }

    /**
     * Utility method used to determine whether the version number as
     * reported by system properties is greater than or equal to a given
     * value.
     *
     * @param value The value to test against.
     * @return false if the version number cannot be parsed as a float,
     *         otherwise the comparison against value.
     */
    private static boolean versionNumberGreaterThanOrEqualTo(float value) {
        try {
            return Float.parseFloat(version) >= value;
        } catch (Exception e) {
            return false;
        }
    }
}
