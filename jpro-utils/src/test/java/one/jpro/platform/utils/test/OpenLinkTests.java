package one.jpro.platform.utils.test;

import one.jpro.platform.utils.OpenLink;
import one.jpro.platform.utils.PlatformUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.URI;

import static org.mockito.Mockito.*;

/**
 * OpenLink tests.
 *
 * @author Besmir Beqiri
 */
public class OpenLinkTests {

    // Mocked static instances for Runtime and PlatformUtils
    private static MockedStatic<Runtime> runtimeMockedStatic;
    private static MockedStatic<PlatformUtils> platformUtilsMockedStatic;
    private static final String TEST_URL = "http://www.example.com";

    private Runtime runtime;

    /**
     * Initializes the static mocks before all tests run.
     */
    @BeforeAll
    public static void init() {
        runtimeMockedStatic = mockStatic(Runtime.class);
        platformUtilsMockedStatic = mockStatic(PlatformUtils.class);
    }

    /**
     * Cleans up the static mocks after all tests have run.
     */
    @AfterAll
    public static void cleanup() {
        if (runtimeMockedStatic != null) {
            runtimeMockedStatic.close();
        }
        if (platformUtilsMockedStatic != null) {
            platformUtilsMockedStatic.close();
        }
    }

    /**
     * Sets up the mock Runtime instance before each test.
     */
    @BeforeEach
    public void setup() {
        // Create a mock Runtime instance
        runtime = mock(Runtime.class);
        // Stub Runtime.getRuntime() to return the mocked Runtime
        runtimeMockedStatic.when(Runtime::getRuntime).thenReturn(runtime);
    }

    @Test
    public void testOpenURLOnMac() throws IOException {
        when(PlatformUtils.isDesktop()).thenReturn(true);
        when(PlatformUtils.isMac()).thenReturn(true);
        when(PlatformUtils.isWindows()).thenReturn(false);
        when(PlatformUtils.isLinux()).thenReturn(false);

        OpenLink.openURL(URI.create(TEST_URL).toURL());

        verify(runtime).exec(new String[]{"open", TEST_URL});
    }

    @Test
    public void testOpenURLOnWindows() throws IOException {
        when(PlatformUtils.isDesktop()).thenReturn(true);
        when(PlatformUtils.isMac()).thenReturn(false);
        when(PlatformUtils.isWindows()).thenReturn(true);
        when(PlatformUtils.isLinux()).thenReturn(false);

        OpenLink.openURL(URI.create(TEST_URL).toURL());

        verify(runtime).exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", TEST_URL});
    }

    @Test
    public void testOpenURLOnLinux() throws IOException {
        when(PlatformUtils.isDesktop()).thenReturn(true);
        when(PlatformUtils.isMac()).thenReturn(false);
        when(PlatformUtils.isWindows()).thenReturn(false);
        when(PlatformUtils.isLinux()).thenReturn(true);

        OpenLink.openURL(URI.create(TEST_URL).toURL());

        verify(runtime).exec(new String[]{"xdg-open", TEST_URL});
    }
}
