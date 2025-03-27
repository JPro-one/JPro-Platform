package one.jpro.platform.utils;

import com.jpro.webapi.WebAPI;
import javafx.scene.input.KeyCode; // Assumes JavaFX is on the classpath


/**
 * A utility class for detecting user platform properties.
 * <p>
 * This class supports both JPro (Web) and Desktop environments.
 * It determines OS types using provided WebAPI values or system properties.
 * </p>
 */
public class UserPlatform {

    private final String platform;

    /**
     * Creates a UserPlatform instance from a WebAPI.
     *
     * @param webAPI the WebAPI instance to extract platform data
     */
    public UserPlatform(WebAPI webAPI) {
        this(platformFrom(webAPI.getPlatform(), webAPI.getPlatformOld()));
    }

    /**
     * Creates a UserPlatform instance using explicit platform strings (for unit tests).
     *
     * @param platform     the primary platform string
     * @param platformOld  the fallback platform string
     */
    private UserPlatform(String platform, String platformOld) {
        this(platformFrom(platform, platformOld));
    }

    /**
     * Creates a UserPlatform instance using the desktop OS property.
     */
    public UserPlatform() {
        this(System.getProperty("os.name"));
        if(WebAPI.isBrowser()) {
            throw new IllegalStateException("UserPlatform() should not be used in a browser environment. Use UserPlatform(WebAPI) instead.");
        }
    }

    /**
     * Used for unit test and example application
     */
    public static UserPlatform simulateDesktop() {
        return new UserPlatform(System.getProperty("os.name"));
    }
    /**
     * Used for unit test and example application
     */
    public static UserPlatform simulateDesktop(String s) {
        return new UserPlatform(System.getProperty("os.name"));
    }

    public static UserPlatform simulateWeb(String platform, String platformOld) {
        return new UserPlatform(platform, platformOld);
    }


    // Private constructor to set the internal platform string.
    private UserPlatform(String platform) {
        this.platform = (platform != null) ? platform : "";
    }

    // Helper method to choose the best available platform string.
    private static String platformFrom(String primary, String fallback) {
        if (primary != null && !primary.isEmpty()) {
            return primary;
        }
        return (fallback != null) ? fallback : "";
    }

    public boolean isWindows() {
        return platform.toLowerCase().contains("win");
    }

    public boolean isMac() {
        return platform.toLowerCase().contains("mac");
    }

    public boolean isLinux() {
        String lower = platform.toLowerCase();
        return lower.contains("nux") || lower.contains("linux");
    }

    public boolean isDesktop() {
        return isWindows() || isMac() || isLinux();
    }

    public boolean isEmbedded() {
        // Implementation can be adjusted as needed.
        return false;
    }

    public boolean isAndroid() {
        return platform.toLowerCase().contains("android");
    }

    public boolean isIOS() {
        String lower = platform.toLowerCase();
        return lower.contains("ios") || lower.contains("iphone") || lower.contains("ipad");
    }

    public boolean isAarch64() {
        return "aarch64".equalsIgnoreCase(System.getProperty("os.arch"));
    }

    /**
     * Determines if the platform is a mobile device.
     *
     * @return {@code true} if Android or iOS, {@code false} otherwise
     */
    public boolean isMobile() {
        return isAndroid() || isIOS();
    }

    /**
     * Determines if the platform is a web (JPro) environment.
     *
     * @return {@code true} if not a Desktop OS, {@code false} otherwise
     */
    /*
    public boolean isWeb() {
        return !isDesktop();
    }*/

    /**
     * Returns the meta key for the current platform.
     * For Mac OS it returns {@code KeyCode.META}, otherwise {@code KeyCode.CONTROL}.
     *
     * @return the meta key code
     */
    public KeyCode getMetaKey() {
        return isMac() ? KeyCode.META : KeyCode.CONTROL;
    }

    /**
     * Returns the meta key for the platform determined by the given WebAPI.
     *
     * @param webAPI the WebAPI to determine the platform
     * @return the meta key code
     */
    public KeyCode getMetaKey(WebAPI webAPI) {
        return isMac(webAPI) ? KeyCode.META : KeyCode.CONTROL;
    }

    /**
     * Determines if the platform from the WebAPI is Windows.
     *
     * @param webAPI the WebAPI instance
     * @return {@code true} if Windows, {@code false} otherwise
     */
    public static boolean isWindows(WebAPI webAPI) {
        String p = platformFrom(webAPI.getPlatform(), webAPI.getPlatformOld());
        return p.toLowerCase().contains("win");
    }

    /**
     * Determines if the platform from the WebAPI is Mac.
     *
     * @param webAPI the WebAPI instance
     * @return {@code true} if Mac, {@code false} otherwise
     */
    public static boolean isMac(WebAPI webAPI) {
        String p = platformFrom(webAPI.getPlatform(), webAPI.getPlatformOld());
        return p.toLowerCase().contains("mac");
    }

    /**
     * Determines if the platform from the WebAPI is Linux.
     *
     * @param webAPI the WebAPI instance
     * @return {@code true} if Linux, {@code false} otherwise
     */
    public static boolean isLinux(WebAPI webAPI) {
        String p = platformFrom(webAPI.getPlatform(), webAPI.getPlatformOld());
        String lower = p.toLowerCase();
        return lower.contains("nux") || lower.contains("linux");
    }

    /**
     * Determines if the platform from the WebAPI is a Desktop OS.
     *
     * @param webAPI the WebAPI instance
     * @return {@code true} if Desktop, {@code false} otherwise
     */
    public static boolean isDesktop(WebAPI webAPI) {
        return isWindows(webAPI) || isMac(webAPI) || isLinux(webAPI);
    }

    /**
     * Determines if the platform from the WebAPI is Mobile.
     *
     * @param webAPI the WebAPI instance
     * @return {@code true} if Mobile, {@code false} otherwise
     */
    public static boolean isMobile(WebAPI webAPI) {
        String p = platformFrom(webAPI.getPlatform(), webAPI.getPlatformOld());
        String lower = p.toLowerCase();
        return lower.contains("android") || lower.contains("ios") ||
                lower.contains("iphone") || lower.contains("ipad");
    }

    /**
     * Determines if the current environment is web-based (JPro).
     * <p>
     * This method checks the system property "javafx.platform" for the value "jpro".
     * </p>
     *
     * @return {@code true} if web-based, {@code false} otherwise
     */
    public static boolean isWeb() {
        return WebAPI.isBrowser();
    }
}
