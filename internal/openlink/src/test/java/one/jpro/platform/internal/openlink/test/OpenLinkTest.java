package one.jpro.platform.internal.openlink.test;

import one.jpro.platform.internal.openlink.OpenLink;
import one.jpro.platform.internal.openlink.util.PlatformUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import static org.mockito.Mockito.*;

/**
 * OpenLink tests.
 *
 * @author Besmir Beqiri
 */
public class OpenLinkTest {

    @Mock
    private static Runtime runtime;

    @BeforeAll
    public static void setUp() {
        mockStatic(Runtime.class);
        mockStatic(PlatformUtils.class);

        runtime = mock(Runtime.class);
        when(Runtime.getRuntime()).thenReturn(runtime);
    }

    @Test
    public void testOpenURLOnMac() throws IOException {
        when(PlatformUtils.isDesktop()).thenReturn(true);
        when(PlatformUtils.isMac()).thenReturn(true);
        when(PlatformUtils.isWindows()).thenReturn(false);
        when(PlatformUtils.isLinux()).thenReturn(false);

        OpenLink.openURL(URI.create("http://www.example.com").toURL());

        verify(runtime).exec(new String[] {"open", "http://www.example.com"});
    }

    @Test
    public void testOpenURLOnWindows() throws IOException {
        when(PlatformUtils.isDesktop()).thenReturn(true);
        when(PlatformUtils.isMac()).thenReturn(false);
        when(PlatformUtils.isWindows()).thenReturn(true);
        when(PlatformUtils.isLinux()).thenReturn(false);

        OpenLink.openURL(URI.create("http://www.example.com").toURL());

        verify(runtime).exec(new String[] {"start", "http://www.example.com"});
    }

    @Test
    public void testOpenURLOnLinux() throws IOException {
        when(PlatformUtils.isDesktop()).thenReturn(true);
        when(PlatformUtils.isMac()).thenReturn(false);
        when(PlatformUtils.isWindows()).thenReturn(false);
        when(PlatformUtils.isLinux()).thenReturn(true);

        OpenLink.openURL(URI.create("http://www.example.com").toURL());

        verify(runtime).exec(new String[] {"xdg-open", "http://www.example.com"});
    }
}
