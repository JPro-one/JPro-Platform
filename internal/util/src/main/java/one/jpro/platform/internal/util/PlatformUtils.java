package one.jpro.platform.internal.util;

/**
 * Platform utilities.
 *
 * @author Besmir Beqiri
 */
public class PlatformUtils {

    private static final String os = System.getProperty("os.name");
    private static final String version = System.getProperty("os.version");
    private static final String arch = System.getProperty("os.arch");
    private static final String javafxPlatform = System.getProperty("javafx.platform");
    private static final boolean embedded = Boolean.getBoolean("com.sun.javafx.isEmbedded");
    private static final boolean ANDROID = "android".equals(javafxPlatform) || "Dalvik".equals(System.getProperty("java.vm.name"));
    private static final boolean WINDOWS = os.startsWith("Windows");
    private static final boolean MAC = os.startsWith("Mac");
    private static final boolean LINUX = os.startsWith("Linux") && !ANDROID;
    private static final boolean IOS = os.startsWith("iOS");

    /**
     * Checks if the current system is a desktop platform.
     *
     * @return <code>true</code> if the current system is a desktop platform, <code>false</code> otherwise.
     */
    public static boolean isDesktop() {
        return isWindows() || isMac() || isLinux();
    }

    /**
     * Returns true if the operating system is a form of Windows.
     *
     * @return <code>true</code> if operating system is Windows, <code>false</code> otherwise.
     */
    public static boolean isWindows(){
        return WINDOWS;
    }

    /**
     * Returns true if the operating system is a form of macOS.
     *
     * @return <code>true</code> if operating system is macOS, <code>false</code> otherwise.
     */
    public static boolean isMac(){
        return MAC;
    }

    /**
     * Returns true if the operating system is a form of Linux.
     *
     * @return <code>true</code> if operating system is Linux, <code>false</code> otherwise.
     */
    public static boolean isLinux() {
        return LINUX;
    }

    /**
     * Returns true if the platform is embedded.
     *
     * @return <code>true</code> if the platform is embedded, <code>false</code> otherwise.
     */
    public static boolean isEmbedded() {
        return embedded;
    }

    /**
     * Returns true if the platform is Android.
     *
     * @return <code>true</code> if the platform is Android, <code>false</code> otherwise.
     */
    public static boolean isAndroid() {
        return ANDROID;
    }

    /**
     * Returns true if the operating system is iOS.
     *
     * @return <code>true</code> if the operating system is iOS, <code>false</code> otherwise.
     */
    public static boolean isIOS(){
        return IOS;
    }

    /**
     * Returns true if the operating system architecture is arm64 (aarch64).
     *
     * @return <code>true</code> if the operating system architecture is arm64, <code>false</code> otherwise.
     */
    public static boolean isAarch64() {
        return arch.equals("aarch64");
    }

    /**
     * Utility method used to determine whether the version number as
     * reported by system properties is greater than or equal to a given
     * value.
     *
     * @param value The value to test against.
     * @return <code>false</code> if the version number cannot be parsed as a float,
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
