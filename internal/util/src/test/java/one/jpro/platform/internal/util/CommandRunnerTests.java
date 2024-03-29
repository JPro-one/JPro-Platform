package one.jpro.platform.internal.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * CommandRunner tests.
 *
 * @author Besmir Beqiri
 */
public class CommandRunnerTests {

    private Logger logger;
    private CommandRunner commandRunner;

    @BeforeEach
    public void setUp() {
        // Initialize with a mocked logger
        logger = mock(Logger.class);
        commandRunner = new CommandRunner(logger);
    }

    @Test
    public void constructorWithArgsTest() {
        String[] args = {"arg1", "arg2"};
        commandRunner = new CommandRunner(logger, args);
        assertThat(commandRunner.getCmdList()).containsExactly(args);
    }

    @Test
    public void addArgTest() {
        String arg = "testArg";
        commandRunner.addArg(arg);
        assertThat(commandRunner.getCmdList()).contains(arg);
    }

    @Test
    public void addMultipleArgsTest() {
        String[] args = {"arg3", "arg4"};
        commandRunner.addArgs(args);
        assertThat(commandRunner.getCmdList()).contains(args);
    }

    @Test
    public void processTest() throws IOException, InterruptedException {
        if (PlatformUtils.isWindows()) {
            commandRunner.addArgs("cmd", "/c", "dir", "/b", "build.gradle");
        } else {
            commandRunner.addArgs("ls", "build.gradle");
        }
        assertThat(commandRunner.run("dir")).isEqualTo(0);
        assertThat(commandRunner.getResponses().size()).isEqualTo(1);
        assertThat(commandRunner.getLastResponse()).isEqualTo("build.gradle");
    }

    @Test
    public void processLogTest() throws IOException, InterruptedException {
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
