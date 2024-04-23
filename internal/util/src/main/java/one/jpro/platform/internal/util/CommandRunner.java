package one.jpro.platform.internal.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Run processes based on command line arguments.
 *
 * @author Besmir Beqiri
 */
public class CommandRunner {

    private final Logger logger;
    private final List<String> args = new ArrayList<>();
    private final Map<String, String> envVars = new HashMap<>();
    private final List<String> secretArgs = new ArrayList<>();
    private final StringBuffer processOutput = new StringBuffer();
    private boolean interactive = false;
    private boolean printToConsole = false;

    /**
     * Initializes CommandRunner with default logger and command line arguments.
     *
     * @param args command line arguments to be executed by the process.
     */
    public CommandRunner(String... args) {
        this(LoggerFactory.getLogger(CommandRunner.class), args);
    }

    /**
     * Initializes CommandRunner with specified logger and command line arguments.
     *
     * @param logger Logger instance for logging process activities.
     * @param args   Command line arguments to be executed by the process.
     */
    public CommandRunner(Logger logger, @NotNull String... args) {
        this.logger = logger;
        Collections.addAll(this.args, args);
    }

    /**
     * When set to true, it will enable user interaction
     * during the process. By default, is false
     *
     * @param interactive a boolean that sets the interactive mode
     */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    /**
     * When set to true, it will enable the process output to be
     * printed to the console. By default, is false
     *
     * @param printToConsole a boolean that sets the print to console mode
     */
    public void setPrintToConsole(boolean printToConsole) {
        this.printToConsole = printToConsole;
    }

    /**
     * Adds a command line argument to the list of existing list of
     * command line arguments
     *
     * @param arg a string passed to the command line arguments
     */
    public void addArg(@NotNull String arg) {
        args.add(arg);
    }

    /**
     * Adds a command line argument to the list of existing list of
     * command line arguments, marking it as secret argument, in
     * order to avoid logging
     *
     * @param arg a string passed to the command line arguments
     */
    public void addSecretArg(@Nullable String arg) {
        secretArgs.add(arg);
        args.add(arg);
    }

    /**
     * Adds a varargs list of arguments to the existing list of
     * command line of arguments
     *
     * @param args varargs list of arguments
     */
    public void addArgs(@NotNull String... args) {
        this.args.addAll(Arrays.asList(args));
    }

    /**
     * Adds a collection of arguments to the existing list of
     * command line of arguments
     *
     * @param args a collection of arguments
     */
    public void addArgs(@NotNull Collection<String> args) {
        this.args.addAll(args);
    }

    /**
     * @return the command line of arguments as a string
     */
    public String getCmd() {
        return String.join(" ", args);
    }

    /**
     * @return the current list of command line arguments
     */
    public List<String> getCmdList() {
        return args;
    }

    /**
     * Adds a pair (key, value) to the environment map of the process.
     *
     * @param key   a string with the environmental variable name
     * @param value a string with the environmental variable value
     */
    public void addToEnv(@NotNull String key, @NotNull String value) {
        envVars.put(key, value);
    }

    /**
     * Runs a process with a given set of command line arguments.
     *
     * @param processName the name of the process
     * @return 0 if the process ends successfully, non-zero values indicate a failure
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the current thread is interrupted by another thread while it is waiting
     */
    public int run(@Nullable String processName) throws IOException, InterruptedException {
        return run(processName, null);
    }

    /**
     * Runs a process with a given set of command line arguments, in a given working directory.
     *
     * @param processName      the name of the process
     * @param workingDirectory a file with the working directory of the process
     * @return 0 if the process ends successfully, non-zero values indicate a failure
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the current thread is interrupted by another thread while it is waiting
     */
    public int run(@Nullable String processName,
                   @Nullable File workingDirectory) throws IOException, InterruptedException {
        Process process = setupProcess(processName, workingDirectory);
        Thread mergeOutputThread = mergeProcessOutput(process.getInputStream());
        int result = process.waitFor();
        mergeOutputThread.join();
        logger.debug("Result for {}: {}", processName, result);
        if (result != 0) logger.error("Process {} failed with result: {}", processName, result);
        return result;
    }

    /**
     * Runs a process asynchronously with a given set of command line arguments.
     * By default, it merges the output of the process.
     *
     * @param processName the name of the process
     * @return the {@link Process} object
     * @throws IOException if an I/O error occurs
     */
    public Process runAsync(@Nullable String processName) throws IOException {
        return runAsync(processName, null, true);
    }

    /**
     * Runs a process asynchronously with a given set of command line arguments, in a given
     * working directory. By default, it merges the output of the process.
     *
     * @param processName      the name of the process
     * @param workingDirectory a file with the working directory of the process
     * @return the {@link Process} object
     * @throws IOException if an I/O error occurs
     */
    public Process runAsync(@Nullable String processName,
                            @Nullable File workingDirectory) throws IOException {
        return runAsync(processName, workingDirectory, true);
    }

    /**
     * Runs a process asynchronously with a given set of command line arguments, in a given
     * working directory.
     *
     * @param processName the name of the process
     * @param workingDirectory a file with the working directory of the process
     * @param mergeOutput a boolean that sets the merge output mode
     * @return the {@link Process} object
     * @throws IOException if an I/O error occurs
     */
    public Process runAsync(@Nullable String processName,
                            @Nullable File workingDirectory,
                            boolean mergeOutput) throws IOException {
        Process process = setupProcess(processName, workingDirectory);
        if (mergeOutput) mergeProcessOutput(process.getInputStream());
        return process;
    }

    /**
     * Runs a process with a given set of command line arguments within a given time frame.
     *
     * @param processName the name of the process
     * @param timeout     the maximum time allowed to run the process in seconds
     * @return true if the process ended successfully, false otherwise
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the current thread is interrupted by another thread while it is waiting
     */
    public boolean runTimed(@Nullable String processName, long timeout) throws IOException, InterruptedException {
        return runTimed(processName, null, timeout);
    }

    /**
     * Runs a process with a given set of command line arguments, in a given
     * working directory, within a given time frame
     *
     * @param processName      the name of the process
     * @param workingDirectory a file with the working directory of the process
     * @param timeout          the maximum time allowed to run the process
     * @return true if the process ended successfully, false otherwise
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the current thread is interrupted by another thread while it is waiting
     */
    public boolean runTimed(@Nullable String processName,
                            @Nullable File workingDirectory, long timeout)
            throws IOException, InterruptedException {
        Process process = setupProcess(processName, workingDirectory);
        Thread logThread = mergeProcessOutput(process.getInputStream());
        boolean result = process.waitFor(timeout, TimeUnit.SECONDS);
        logThread.join();
        logger.debug("Result for {}: {}", processName, result);
        if (!result) logger.error("Process {} failed with result: {}", processName, result);
        return result;
    }

    /**
     * Gets the response of the process as single string.
     *
     * @return a single string with the whole output of the process
     */
    @Nullable
    public String getResponse() {
        return processOutput.length() > 0 ?
                processOutput.toString().replaceAll("\n", "") : null;
    }

    /**
     * Gets the response of the process as list of lines.
     *
     * @return a list with all the lines of the output
     */
    @NotNull
    public List<String> getResponses() {
        return processOutput.length() > 0 ?
                Arrays.asList(processOutput.toString().split("\n")) : Collections.emptyList();
    }

    /**
     * Gets the last line of the output process.
     *
     * @return a string with the last line of the output
     */
    public String getLastResponse() {
        String[] responses = processOutput.toString().split("\n");
        return responses.length > 0 ? responses[responses.length - 1] : "";
    }

    /**
     * Prepares the process for execution.
     *
     * @param processName the name of the process
     * @param directory   the working directory of the process
     * @return the process object
     * @throws IOException if an I/O error occurs
     */
    private Process setupProcess(final String processName, final File directory) throws IOException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("No command line arguments provided");
        }

        ProcessBuilder pb = new ProcessBuilder(args).redirectErrorStream(true);
        if (interactive) pb.inheritIO();
        if (directory != null) pb.directory(directory);
        envVars.forEach(pb.environment()::put);
        processOutput.setLength(0);
        String sanitizedCmd = args.stream()
                .map(arg -> secretArgs.contains(arg) ? arg.charAt(0) + "******" : arg)
                .collect(Collectors.joining(" "));
        logger.debug("Command for {}: {}", processName, sanitizedCmd);
        return pb.start();
    }

    /**
     * Captures the process output, logging it and appending to an internal buffer.
     *
     * @param inputStream the input stream of the process
     * @return a thread that captures the process output
     */
    private Thread mergeProcessOutput(final InputStream inputStream) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                reader.lines().forEach(line -> {
                    processOutput.append(line).append("\n");
                    if (printToConsole) {
                        System.out.println(line);
                    }
                    logger.debug(line);
                });
            } catch (IOException ex) {
                logger.error("Error reading process output", ex);
            }
        });
        thread.start();
        return thread;
    }
}
