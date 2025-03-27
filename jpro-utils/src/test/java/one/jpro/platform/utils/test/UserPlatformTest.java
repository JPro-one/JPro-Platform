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
    }

    @Test
    public void testMac2() {
        var platform = UserPlatform.simulateWeb(null, "MacIntel");
        Assertions.assertTrue(platform.isMac());
        Assertions.assertFalse(platform.isWindows());
        Assertions.assertFalse(platform.isLinux());
        Assertions.assertTrue(platform.isDesktop());
    }

    @Test
    public void testMac3() {
        var platform = UserPlatform.simulateDesktop("Mac OS X");
        Assertions.assertTrue(platform.isMac());
        Assertions.assertFalse(platform.isWindows());
        Assertions.assertFalse(platform.isLinux());
        Assertions.assertTrue(platform.isDesktop());

        Assertions.assertEquals(platform.getMetaKey() == KeyCode.COMMAND);
    }
}
