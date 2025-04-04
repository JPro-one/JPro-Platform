package one.jpro.platform.utils.test;

import javafx.scene.input.KeyCode;
import one.jpro.platform.utils.UserPlatform;
import org.junit.jupiter.api.
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserPlatformTest {

    @Test
    public void printVariables() {
        System.out.println(System.getProperty("os.name"));
        System.out.println(System.getProperty("os.arch"));
        System.out.println(System.getProperty("os.version"));
    }

    @Test
    public void testMac1() {
        var platform = UserPlatform.simulateWeb("macOS", "MacIntel");
        assertTrue(platform.isMac());
        assertFalse(platform.isWindows());
        assertFalse(platform.isLinux());
        assertEquals(platform.getModifierKey(), KeyCode.META);
    }

    @Test
    public void testMac2() {
        var platform = UserPlatform.simulateWeb(null, "MacIntel");
        assertTrue(platform.isMac());
        assertFalse(platform.isWindows());
        assertFalse(platform.isLinux());
        assertEquals(platform.getModifierKey(), KeyCode.META);
    }

    @Test
    public void testMac3() {
        var platform = UserPlatform.simulateNative("Mac OS X");
        assertTrue(platform.isMac());
        assertFalse(platform.isWindows());
        assertFalse(platform.isLinux());
        assertFalse(platform.isMobile());
        assertEquals(platform.getModifierKey(), KeyCode.META);
    }

    @Test
    public void testWin1() {
        var platform = UserPlatform.simulateWeb("Windows", "Win32");
        assertFalse(platform.isMac());
        assertTrue(platform.isWindows());
        assertFalse(platform.isLinux());
        assertEquals(platform.getModifierKey(), KeyCode.CONTROL);
    }
    @Test
    public void testWin2() {
        var platform = UserPlatform.simulateWeb(null, "Win32");
        assertFalse(platform.isMac());
        assertTrue(platform.isWindows());
        assertFalse(platform.isLinux());
        assertEquals(platform.getModifierKey(), KeyCode.CONTROL);
    }

    @Test
    public void testIphone() {
        var platform = UserPlatform.simulateWeb(null, "iPhone");
        assertFalse(platform.isMac());
        assertFalse(platform.isWindows());
        assertFalse(platform.isLinux());
    }
}
