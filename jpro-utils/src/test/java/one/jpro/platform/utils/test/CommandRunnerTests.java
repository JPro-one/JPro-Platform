package one.jpro.platform.utils.test;

import one.jpro.platform.utils.CommandRunner;
import one.jpro.platform.utils.PlatformUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * CommandRunner tests.
 *
 * @author Besmir Beqiri
 */
public class CommandRunnerTests {

    private Logger logger;
    private CommandRunner commandRunner;
    private File mockFile;

    @BeforeEach
    public void setUp() {
        // Initialize with a mocked logger
        logger = mock(Logger.class);
        commandRunner = new CommandRunner(logger);
        mockFile = mock(File.class);
    }

    @Test
    public void constructorWithArgs() {
        String[] args = {"arg1", "arg2"};
        commandRunner = new CommandRunner(logger, args);
        assertThat(commandRunner.getCmdList()).containsExactly(args);
    }

    @Test
    public void addArg() {
        String arg = "testArg";
        commandRunner.addArg(arg);
        assertThat(commandRunner.getCmdList()).contains(arg);
    }

    @Test
    public void addMultipleArgs() {
        String[] args = {"arg3", "arg4"};
        commandRunner.addArgs(args);
        assertThat(commandRunner.getCmdList()).contains(args);
    }

    @Test
    public void runAsync() throws IOException {
        if (PlatformUtils.isWindows()) {
            commandRunner.addArgs("cmd", "/c", "dir", "/b", "build.gradle");
        } else {
            commandRunner.addArgs("ls", "build.gradle");
        }
        Process process = commandRunner.runAsync("async-ls");
        assertThat(process.getClass()).isAssignableTo(Process.class);
    }

    @Test
    public void runAsyncWithMockDirectoryThrowsException() {
        if (PlatformUtils.isWindows()) {
            commandRunner.addArgs("cmd", "/c", "dir", "/b", "build.gradle");
            assertThatThrownBy(() -> commandRunner.runAsync("async-cmd-dir", mockFile))
                    .hasMessageContaining("Cannot run program \"cmd\"")
                    .hasMessageContaining("CreateProcess error=267, The directory name is invalid")
                    .hasRootCauseMessage("CreateProcess error=267, The directory name is invalid")
                    .hasCauseInstanceOf(IOException.class);
        } else {
            commandRunner.addArgs("ls", "build.gradle");
            assertThatThrownBy(() -> commandRunner.runAsync("async-ls", mockFile))
                    .hasMessageContaining("Cannot run program \"ls\"")
                    .hasMessageContaining("error=2")
                    .hasMessageContaining("No such file or directory")
                    //.hasRootCauseMessage("error=2, No such file or directory") // Dependent on OS and or JavaVersion
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void runAsyncWithOutput() throws IOException, InterruptedException {
        if (PlatformUtils.isWindows()) {
            commandRunner.addArgs("cmd", "/c", "dir", "/b", "build.gradle");
        } else {
            commandRunner.addArgs("ls", "build.gradle");
        }
        Process process = commandRunner.runAsync("async-dir-list");
        int result = process.waitFor();
        assertThat(result).isEqualTo(0); // Successful execution
        Thread.sleep(TimeUnit.SECONDS.toMillis(3)); // Wait for the process output to be captured
        assertThat(commandRunner.getResponses().size()).isEqualTo(1);
        assertThat(commandRunner.getLastResponse()).isEqualTo("build.gradle");
    }

    @Test
    public void runAsyncWithDirectoryAndOutput() throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("command-runner-tests");
        if (PlatformUtils.isWindows()) {
            commandRunner.addArgs("cmd", "/c", "mkdir", "runner");
        } else {
            commandRunner.addArgs("mkdir", "runner");
        }
        Process process = commandRunner.runAsync("async-dir-list", tempDir.toFile());
        int result = process.waitFor();
        assertThat(result).isEqualTo(0); // Successful execution
        assertThat(commandRunner.getLastResponse()).isEqualTo("");
    }

    @Test
    public void runWithoutArgs() {
        assertThatThrownBy(() -> commandRunner.run("no-args"))
                .hasMessage("No command line arguments provided");
    }

    @Test
    public void runWithResult() throws IOException, InterruptedException {
        if (PlatformUtils.isWindows()) {
            commandRunner.addArgs("cmd", "/c", "dir", "/b", "build.gradle");
        } else {
            commandRunner.addArgs("ls", "build.gradle");
        }
        assertThat(commandRunner.run("dir-list")).isEqualTo(0);
        assertThat(commandRunner.getResponses().size()).isEqualTo(1);
        assertThat(commandRunner.getLastResponse()).isEqualTo("build.gradle");
    }

    @Test
    public void runWithDirectoryAndResult() throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("command-runner-tests");
        String output;
        if (PlatformUtils.isWindows()) {
            commandRunner.addArgs("cmd", "/c", "mkdir", "runner");
            output = "A subdirectory or file runner already exists.";
        } else {
            commandRunner.addArgs("mkdir", "runner");
            output = "File exists";
        }
        assertThat(commandRunner.run("mkdir", tempDir.toFile())).isEqualTo(0);
        assertThat(commandRunner.getResponses().size()).isEqualTo(0);
        assertThat(commandRunner.getLastResponse()).isEqualTo("");

        assertThat(commandRunner.run("mkdir", tempDir.toFile())).isEqualTo(1);
        assertThat(commandRunner.getLastResponse()).endsWith(output);
    }
}
