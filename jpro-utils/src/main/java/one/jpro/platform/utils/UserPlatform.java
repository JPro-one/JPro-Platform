package one.jpro.platform.utils;

import com.jpro.webapi.WebAPI;
import javafx.scene.input.KeyCode; // Assumes JavaFX is on the classpath
import javafx.scene.input.KeyCombination;


/**
 * A utility class for detecting user platform properties.
 * <p>
 * This class supports both JPro (Web) and Desktop environments.
 * It determines OS types using provided WebAPI values or system properties.
 */
public class UserPlatform {

    private final String platform;
    private final WebAPI webAPI;
    private final boolean isNative;

    /**
     * Creates a UserPlatform instance from a WebAPI.
     *
     * @param webAPI the WebAPI instance to extract platform data
     */
    public UserPlatform(WebAPI webAPI) {
        this(platformFrom(webAPI), webAPI, !WebAPI.isBrowser());
    }

    private UserPlatform(String platform, WebAPI webAPI, boolean isNative) {
        this.platform = platform;
        this.isNative = isNative;
        this.webAPI = webAPI;
    }

    /**
     * Used for unit test and example application
     */
    public static UserPlatform simulateNative() {
        return new UserPlatform(System.getProperty("os.name"), null, true);
    }

    /**
     * Used for unit test and example application
     */
    public static UserPlatform simulateNative(String s) {
        return new UserPlatform(s, null, true);
    }

    /**
     * Used for unit test and example application
     */
    public static UserPlatform simulateWeb(String platform, String platformOld) {
        return new UserPlatform(platformFrom(platform, platformOld), null, false);
    }

    private static String platformFrom(WebAPI webAPI) {
        if (WebAPI.isBrowser()) {
            return platformFrom(webAPI.getPlatform(), webAPI.getPlatformOld());
        } else {
            return System.getProperty("os.name");
        }
    }

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

    /**
     * Determines if the platform is a web (JPro) environment.
     *
     * @return {@code true} if not a Desktop OS, {@code false} otherwise
     */
    public boolean isWeb() {
        return WebAPI.isBrowser();
    }

    /**
     * Determines if the platform is a mobile environment.
     *
     * @return {@code true} if mobile, {@code false} otherwise
     */
    public boolean isMobile() {
        if (isNative) {
            return PlatformUtils.isAndroid() || PlatformUtils.isIOS();
        } else {
            return webAPI.isMobile();
        }
    }

    /**
     * Returns the modifier key for the current platform.
     * For macOS, it returns {@code KeyCode.META}, otherwise {@code KeyCode.CONTROL}.
     *
     * @return the meta key code
     */
    public KeyCode getModifierKey() {
        return isMac() ? KeyCode.META : KeyCode.CONTROL;
    }

    /**
     * Returns the modifier key combination for the current platform.
     * For macOS, it returns {@code KeyCombination.META_DOWN}, otherwise {@code KeyCombination.CONTROL_DOWN}.
     *
     * @return the meta key code
     */
    public KeyCombination.Modifier getModifierKeyCombination() {
        return isMac() ? KeyCombination.META_DOWN : KeyCombination.CONTROL_DOWN;
    }

    /**
     * Returns the modifier key for the platform determined by the given WebAPI.
     *
     * @param webAPI the WebAPI to determine the platform
     * @return the meta key code
     */
    public static KeyCode getModifierKey(WebAPI webAPI) {
        return new UserPlatform(webAPI).getModifierKey();
    }

    /**
     * Returns the modifier key combination for the platform determined by the given WebAPI.
     *
     * @param webAPI the WebAPI to determine the platform
     * @return the meta key code
     */
    public static KeyCombination.Modifier getModifierKeyCombination(WebAPI webAPI) {
        return new UserPlatform(webAPI).getModifierKeyCombination();
    }

    /**
     * Determines if the platform from the WebAPI is Windows.
     *
     * @param webAPI the WebAPI instance
     * @return {@code true} if Windows, {@code false} otherwise
     */
    public static boolean isWindows(WebAPI webAPI) {
        return new UserPlatform(webAPI).isWindows();
    }

    /**
     * Determines if the platform from the WebAPI is Mac.
     *
     * @param webAPI the WebAPI instance
     * @return {@code true} if Mac, {@code false} otherwise
     */
    public static boolean isMac(WebAPI webAPI) {
        return new UserPlatform(webAPI).isMac();
    }

    /**
     * Determines if the platform from the WebAPI is Linux.
     *
     * @param webAPI the WebAPI instance
     * @return {@code true} if Linux, {@code false} otherwise
     */
    public static boolean isLinux(WebAPI webAPI) {
        return new UserPlatform(webAPI).isLinux();
    }

    /**
     * Determines if the current environment is web-based (JPro).
     * <p>
     * This method checks the system property "javafx.platform" for the value "jpro".
     *
     * @return {@code true} if web-based, {@code false} otherwise
     */
    public static boolean isWeb(WebAPI webAPI) {
        return WebAPI.isBrowser();
    }

    /**
     * Determines if the platform from the WebAPI is mobile.
     *
     * @param webAPI the WebAPI instance
     * @return {@code true} if mobile, {@code false} otherwise
     */
    public static boolean isMobile(WebAPI webAPI) {
        return new UserPlatform(webAPI).isMobile();
    }
}
