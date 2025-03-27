package one.jpro.platform.utils.test;

import javafx.scene.input.KeyCode;
import one.jpro.platform.utils.UserPlatform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertTrue(platform.isMac());
        Assertions.assertFalse(platform.isWindows());
        Assertions.assertFalse(platform.isLinux());
        Assertions.assertTrue(platform.isDesktop());
        Assertions.assertEquals(platform.getModifierKey(), KeyCode.META);
    }

    @Test
    public void testMac2() {
        var platform = UserPlatform.simulateWeb(null, "MacIntel");
        Assertions.assertTrue(platform.isMac());
        Assertions.assertFalse(platform.isWindows());
        Assertions.assertFalse(platform.isLinux());
        Assertions.assertTrue(platform.isDesktop());
        Assertions.assertEquals(platform.getModifierKey(), KeyCode.META);
    }

    @Test
    public void testMac3() {
        var platform = UserPlatform.simulateDesktop("Mac OS X");
        Assertions.assertTrue(platform.isMac());
        Assertions.assertFalse(platform.isWindows());
        Assertions.assertFalse(platform.isLinux());
        Assertions.assertTrue(platform.isDesktop());
        Assertions.assertEquals(platform.getModifierKey(), KeyCode.META);
    }

    @Test
    public void testWin1() {
        var platform = UserPlatform.simulateWeb("Windows", "Win32");
        Assertions.assertFalse(platform.isMac());
        Assertions.assertTrue(platform.isWindows());
        Assertions.assertFalse(platform.isLinux());
        Assertions.assertTrue(platform.isDesktop());
        Assertions.assertEquals(platform.getModifierKey(), KeyCode.CONTROL);
    }
    @Test
    public void testWin2() {
        var platform = UserPlatform.simulateWeb(null, "Win32");
        Assertions.assertFalse(platform.isMac());
        Assertions.assertTrue(platform.isWindows());
        Assertions.assertFalse(platform.isLinux());
        Assertions.assertTrue(platform.isDesktop());
        Assertions.assertEquals(platform.getModifierKey(), KeyCode.CONTROL);
    }

    @Test
    public void testIphone() {
        var platform = UserPlatform.simulateWeb(null, "iPhone");
        Assertions.assertFalse(platform.isMac());
        Assertions.assertFalse(platform.isWindows());
        Assertions.assertFalse(platform.isLinux());
        Assertions.assertTrue(platform.isIOS());
        Assertions.assertFalse(platform.isDesktop());
    }

    /*
    @Test
    public void testAndroid() {
        // TODO
        // From android, not really useful.
        var platform = UserPlatform.simulateWeb(null, "Linux armv81");
        Assertions.assertFalse(platform.isMac());
        Assertions.assertFalse(platform.isWindows());
        Assertions.assertTrue(platform.isLinux());
        Assertions.assertFalse(platform.isIOS());
        Assertions.assertFalse(platform.isDesktop());
    }*/
}
