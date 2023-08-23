package one.jpro.platform.media.recorder.impl;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import one.jpro.platform.media.MediaSource;
import one.jpro.platform.media.event.MediaRecorderEvent;
import one.jpro.platform.media.recorder.MediaRecorder;
import one.jpro.platform.media.recorder.MediaRecorderException;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * {@link MediaRecorder} implementation for the desktop.
 *
 * @author Besmir Beqiri
 */
public final class FXMediaRecorder extends BaseMediaRecorder {

    private final Logger log = LoggerFactory.getLogger(FXMediaRecorder.class);

    private static final Path RECORDING_PATH = Path.of(System.getProperty("user.home"),
            ".jpro", "video", "capture");
    private static final String DIVIDER_LINE =
            CharBuffer.allocate(80).toString().replace( '\0', '*' );

    // Video resources
    private static final int WEBCAM_DEVICE_INDEX = 0;
    private static final int FRAME_RATE = 30;
    private static final String MP4_FILE_EXTENSION = ".mp4";
    private final FrameGrabber webcamGrabber;
    private final JavaFXFrameConverter frameConverter;
    private final ImageView frameView;
    private double frameRate;

    // Audio resources
    private static final int DEFAULT_AUDIO_SAMPLE_RATE = 44100; // 44.1 KHz
    private static final int DEFAULT_AUDIO_CHANNELS = 0; // no audioHz
    private static final int DEFAULT_AUDIO_FRAME_SIZE = 1; // 1 byte
    private AudioFormat audioFormat;
    private TargetDataLine micLine;
    private int audioSampleRate;
    private int audioNumChannels;

    // Storage resources
    private FFmpegFrameRecorder recorder;
    private Path tempVideoFile;

    // Recording state
    private volatile boolean recordingStarted = false;
    private volatile boolean recordingStopped = false;

    // Concurrency and locking resources
    private final ThreadGroup scheduledThreadGroup = new ThreadGroup("Media Recorder thread pool");
    private int threadCounter;
    private final ExecutorService videoExecutorService;
    private final ScheduledExecutorService audioExecutorService;
    private final ExecutorService startStopRecordingExecutorService;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

    public FXMediaRecorder() {
        // Set native log level to error
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);

        final ThreadFactory threadFactory = run -> {
            final Thread thread = new Thread(scheduledThreadGroup, run);
            thread.setName("Media Recorder Thread " + threadCounter++);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            return thread;
        };
        videoExecutorService = Executors.newSingleThreadExecutor(threadFactory);
        audioExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        startStopRecordingExecutorService = Executors.newSingleThreadExecutor(threadFactory);

        // Initialize webcam frame grabber
        webcamGrabber = (isOsWindows()) ? new VideoInputFrameGrabber(WEBCAM_DEVICE_INDEX)
                : new OpenCVFrameGrabber(WEBCAM_DEVICE_INDEX);
        // Frame to JavaFX image converter
        frameConverter = new JavaFXFrameConverter();
        // Use ImageView to show camera grabbed frames
        frameView = new ImageView();

        // Stop and release native resources on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopRecording();
            release();

            // delete temporary video files
            if (Files.exists(RECORDING_PATH)) {
                try (Stream<Path> pathStream = Files.walk(RECORDING_PATH)) {
                    pathStream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        }));
    }

    ImageView getCameraView() {
        return frameView;
    }

    @Override
    public void enable() {
        if (recorderReady) {
            log.info("Media recorder is already enabled.");
        } else {
            log.debug("Enabling media recorder...");
            // Camera frame grabber runnable
            final Runnable frameGrabber = () -> {
                try {
                    // start the video capture
                    webcamGrabber.start();
                    frameRate = (webcamGrabber.getFrameRate() < FRAME_RATE) ? FRAME_RATE : webcamGrabber.getFrameRate();
                    printCaptureDeviceDescription();
                } catch (FrameGrabber.Exception ex) {
                    setError("Exception during the enabling of video camera stream.", ex);
                    release();
                }

                try {
                    // start the audio capture
                    enableAudioCapture();
                } catch (LineUnavailableException ex) {
                    setError("Exception on creating audio input line from the microphone.", ex);
                    if (micLine != null) {
                        micLine.close();
                    }
                }

                // Set recorder ready
                recorderReady = true;
                Platform.runLater(() -> {
                    // Set status to ready
                    setStatus(Status.READY);

                    // Fire ready event
                    Event.fireEvent(FXMediaRecorder.this,
                            new MediaRecorderEvent(FXMediaRecorder.this,
                                    MediaRecorderEvent.MEDIA_RECORDER_READY));
                });

                // Start the camera frame grabbing, showing and recording task
                try {
                    while (recorderReady) {
                        // effectively grab a single frame
                        final Frame frame;
                        readLock.lock();
                        try {
                            frame = webcamGrabber.grab();
                        } finally {
                            readLock.unlock();
                        }

                        if (frame != null) {
                            // convert and show the frame
                            updateCameraView(frameView, frameConverter.convert(frame));

                            // write the webcam frame if recording started
                            writeVideoFrame(frame);
                        }
                    }
                } catch (FrameGrabber.Exception ex) {
                    setError("Exception during camera stream frame grabbing.", ex);
                    release();
                }
            };

            videoExecutorService.execute(frameGrabber);
        }
    }

    /**
     * Records a video frame.
     *
     * @param frame the frame to record
     */
    private void writeVideoFrame(Frame frame) {
        if (!recordingStopped && recorder != null) {
            writeLock.lock();
            try {
                recorder.record(frame);
            } catch (FFmpegFrameRecorder.Exception ex) {
                setError("Exception during video frame recording.", ex);
            } finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * Records an audio data.
     *
     * @throws LineUnavailableException if the mic line is unavailable
     */
    private void enableAudioCapture() throws LineUnavailableException {
        final Mixer.Info micDevice = getDefaultMicDevice();
        if (micDevice != null) {
            final Mixer micMixer = AudioSystem.getMixer(micDevice);
            // Audio format: 44.1k sample rate, 16 bits, mono, signed, little endian
            audioFormat = new AudioFormat(DEFAULT_AUDIO_SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

            if (micMixer.isLineSupported(dataLineInfo)) {
                micLine = (TargetDataLine) micMixer.getLine(dataLineInfo);
                micLine.open(audioFormat);
                micLine.start();

                audioSampleRate = (int) audioFormat.getSampleRate();
                audioNumChannels = audioFormat.getChannels();
            }

            final Runnable audioSampleGrabber = () -> {
                if (recordingStarted) {
                    // Initialize audio buffer
                    final int audioBufferSize = audioSampleRate * audioNumChannels;
                    final byte[] audioBytes = new byte[audioBufferSize];

                    // Read from the line
                    int nBytesRead = 0;
                    while (nBytesRead == 0) {
                        nBytesRead = micLine.read(audioBytes, 0, micLine.available());
                    }

                    final int nSamplesRead = nBytesRead / 2;
                    final short[] samples = new short[nSamplesRead];

                    final ByteOrder byteOrder = audioFormat.isBigEndian() ?
                            ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;

                    // Wrap our short[] into a ShortBuffer
                    ByteBuffer.wrap(audioBytes).order(byteOrder).asShortBuffer().get(samples);
                    ShortBuffer samplesBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);

//                    log.trace("Record audio samples: {}", nSamplesRead);

                    // recorder audio data
                    if (!recordingStopped && recorder != null) {
                        writeLock.lock();
                        try {
                            recorder.recordSamples(audioSampleRate, audioNumChannels, samplesBuff);
                        } catch (FFmpegFrameRecorder.Exception ex) {
                            setError("Exception on recording the audio samples.", ex);
                        } finally {
                            writeLock.unlock();
                        }
                    }
                }
            };

            final long period = (long) (1000.0 / frameRate);
            audioExecutorService.scheduleAtFixedRate(audioSampleGrabber, 0, period, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Retries the list of available audio input devices and returns the default microphone device.
     *
     * @return the default microphone device
     */
    private Mixer.Info getDefaultMicDevice() {
        final Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            final Mixer mixer = AudioSystem.getMixer(info);
            final Line.Info[] lineInfos = mixer.getTargetLineInfo();
            // Only prints out info is it is a Microphone
            if (lineInfos.length >= 1 && lineInfos[0].getLineClass().equals(TargetDataLine.class)) {
                log.debug(DIVIDER_LINE);
                for (Line.Info lineInfo : lineInfos) {
                    log.debug("Mic Line Name: " + info.getName()); // The audio device name
                    log.debug("Mic Line Description: " + info.getDescription()); // The type of audio device
                    printSupportedAudioFormats(lineInfo);
                }
                log.debug(DIVIDER_LINE);
                return info;
            }
        }
        return null;
    }

    @Override
    public void start() {
        if (getStatus().equals(Status.INACTIVE) || getStatus().equals(Status.READY)) {
            final Runnable startRecordingRunnable = () -> {
                // Start the recording if the camera is enabled
                if (recorderReady) {
                    tempVideoFile = createTempFilename("video_", MP4_FILE_EXTENSION);

                    recorder = new FFmpegFrameRecorder(tempVideoFile.toString(),
                            webcamGrabber.getImageWidth(), webcamGrabber.getImageHeight());
                    recorder.setInterleaved(true);
                    recorder.setVideoOption("tune", "zerolatency"); // low latency for webcam streaming
                    recorder.setVideoOption("preset", "ultrafast"); // low cpu usage for the encoder
                    recorder.setVideoOption("crf", "28");           // video quality
//                    recorder.setVideoBitrate(webcamGrabber.getVideoBitrate());
                    recorder.setVideoBitrate(8 * 1024 * 1024);      // 8 Mbps for 1080p
                    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                    recorder.setFormat("mp4");
                    recorder.setFrameRate(frameRate);
                    recorder.setGopSize((int) (frameRate * 2));
                    recorder.setAudioOption("crf", "0");            // no variable bitrate audio
                    recorder.setAudioQuality(0);                    // highest quality
//                    recorder.setAudioBitrate(getAudioSampleRate() * getFrameSize() * getAudioChannels());
                    recorder.setAudioBitrate(192000);               // 192 kbps
                    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                    recorder.setSampleRate(getAudioSampleRate());
                    recorder.setAudioChannels(getAudioChannels());

                    try {
                        // Enable recording
                        recorder.start();
                        recordingStopped = false;
                        recordingStarted = true;

                        Platform.runLater(() -> {
                            // Set status to recording
                            setStatus(Status.RECORDING);

                            // Fire start event
                            Event.fireEvent(FXMediaRecorder.this,
                                    new MediaRecorderEvent(FXMediaRecorder.this,
                                            MediaRecorderEvent.MEDIA_RECORDER_START));
                        });
                    } catch (FFmpegFrameRecorder.Exception ex) {
                        setError("Exception on starting the audio/video recorder.", ex);
                    }
                } else {
                    log.info("Please, enable the camera first!");
                }
            };

            startStopRecordingExecutorService.execute(startRecordingRunnable);
        } else if (getStatus().equals(Status.PAUSED)) {
            if (recorderReady) {
                // enable recording
                recordingStarted = true;

                // Set status to recording
                setStatus(Status.RECORDING);

                // Fire start event
                Event.fireEvent(FXMediaRecorder.this,
                        new MediaRecorderEvent(FXMediaRecorder.this,
                                MediaRecorderEvent.MEDIA_RECORDER_RESUME));
            }
        }
    }

    @Override
    public void pause() {
        // Disable recording
        recordingStarted = false;

        // Set status to paused
        setStatus(Status.PAUSED);

        // Fire start event
        Event.fireEvent(FXMediaRecorder.this,
                new MediaRecorderEvent(FXMediaRecorder.this,
                        MediaRecorderEvent.MEDIA_RECORDER_PAUSE));
    }

    @Override
    public void stop() {
        final Runnable startRecordingRunnable = () -> {
            stopRecording();

            Platform.runLater(() -> {
                // Set the media source
                setMediaSource(new MediaSource(tempVideoFile.toUri().toString()));

                // Set status to inactive
                setStatus(Status.INACTIVE);

                // Fire start event
                Event.fireEvent(FXMediaRecorder.this,
                        new MediaRecorderEvent(FXMediaRecorder.this,
                                MediaRecorderEvent.MEDIA_RECORDER_STOP));
            });
        };
        startStopRecordingExecutorService.execute(startRecordingRunnable);
    }

    /**
     * Stop the recording and closes the file.
     */
    private void stopRecording() {
        // Stop recording
        recordingStarted = false;
        recordingStopped = true;

        // Release video recorder resources
        if (recorder != null) {
            writeLock.lock();
            try {
                recorder.close(); // This call stops the recorder and releases all resources used by it.
            } catch (FrameRecorder.Exception ex) {
                setError("Exception on stopping the audio/video recorder", ex);
            } finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * Stop the acquisition from the camera and microphone and release all the resources.
     */
    public void release() {
        recorderReady = false;
        releaseVideoResources();
        releaseAudioResources();
    }

    /**
     * Release the video resources.
     */
    private void releaseVideoResources() {
        if (videoExecutorService != null && !videoExecutorService.isShutdown()) {
            try {
                // release video grabber
                if (webcamGrabber != null) {
                    webcamGrabber.release();
                }

                // stop the video recoding service
                videoExecutorService.shutdown();
                //noinspection ResultOfMethodCallIgnored
                videoExecutorService.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (FrameGrabber.Exception | InterruptedException ex) {
                setError("Exception in stopping the video frame capture service.", ex);
            }
        }
    }

    /**
     * Release the audio resources.
     */
    private void releaseAudioResources() {
        if (audioExecutorService != null && !audioExecutorService.isShutdown()) {
            try {
                // release audio grabber
                if (micLine != null) {
                    micLine.close();
                }

                // stop the audio recording service
                audioExecutorService.shutdown();
                //noinspection ResultOfMethodCallIgnored
                audioExecutorService.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                setError("Exception in stopping the audio frame capture service.", ex);
            }
        }
    }

    /**
     * Obtains the audio sample rate from the specified audio format
     * used for recoding. If not specified, then default value (44100) is returned.
     *
     * @return the audio sample rate
     */
    private int getAudioSampleRate() {
        if (audioFormat == null) return DEFAULT_AUDIO_SAMPLE_RATE;

        final int sampleRate = (int) audioFormat.getSampleRate();
        return (sampleRate == AudioSystem.NOT_SPECIFIED) ? DEFAULT_AUDIO_SAMPLE_RATE : sampleRate;
    }

    /**
     * Obtains the number of audio channels from the specified audio format
     * used for recoding. If not specified, then <code>0</code> is returned,
     * meaning there no audio input device available.
     *
     * @return the number of audio channels (1 for mono, 2 for stereo, etc.)
     */
    private int getAudioChannels() {
        if (audioFormat == null) return DEFAULT_AUDIO_CHANNELS;

        final int audioChannels = audioFormat.getChannels();
        return (audioChannels == AudioSystem.NOT_SPECIFIED) ? DEFAULT_AUDIO_CHANNELS : audioChannels;
    }

    /**
     * Obtains the frame size in bytes. For compressed formats, the return value is
     * the frame size of the uncompressed audio data. {@code AudioSystem.NOT_SPECIFIED}
     * is returned when the frame size is not defined for this audio format.
     *
     * @return the number of bytes per frame, or {@code AudioSystem.NOT_SPECIFIED}
     */
    private int getFrameSize() {
        if (audioFormat == null) return DEFAULT_AUDIO_FRAME_SIZE;

        final int frameSize = audioFormat.getFrameSize();
        return (frameSize == AudioSystem.NOT_SPECIFIED) ? DEFAULT_AUDIO_FRAME_SIZE : frameSize;
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view  the {@link ImageView} to update
     * @param image the {@link Image} to show
     */
    private void updateCameraView(ImageView view, Image image) {
        Platform.runLater(() -> view.setImage(image));
    }

    /**
     * Create a random filename with the given prefix and postfix.
     *
     * @param prefix  the filename prefix
     * @param postfix the filename postfix
     * @return a string containing the entire path with the created filename
     */
    private Path createTempFilename(final String prefix, final String postfix) {
        // Generate a random filename with the given prefix and postfix
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        String filename = "vid_" + random.nextInt(0, Integer.MAX_VALUE);
        if (prefix != null) {
            filename = prefix + random.nextInt(0, Integer.MAX_VALUE);
        }
        if (postfix != null) {
            filename += postfix;
        }

        final Path tempFile = RECORDING_PATH.resolve(filename);
        final Path parentDir = tempFile.getParent();
        if (Files.notExists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        return tempFile;
    }

    /**
     * Check if the OS is Windows.
     *
     * @return true if the OS is Windows, false otherwise
     */
    private boolean isOsWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Set the error message and exception.
     * This method makes sure that {@link #errorProperty()} is updated in the JavaFX Application Thread.
     *
     * @param message the error message
     * @param ex      the exception
     */
    private void setError(String message, Exception ex) {
        if (Platform.isFxApplicationThread()) {
            setError(new MediaRecorderException(message, ex));
        } else {
            Platform.runLater(() -> setError(new MediaRecorderException(message, ex)));
        }
        log.error(message, ex);
    }

    /**
     * Print to terminal the capture device description.
     */
    private void printCaptureDeviceDescription() {
        log.debug(DIVIDER_LINE);
        if (isOsWindows()) {
            try {
                log.debug("Capture devices: " + Arrays.toString(VideoInputFrameGrabber.getDeviceDescriptions()));
            } catch (FrameGrabber.Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        log.debug("Capture Device Info:");
        log.debug("Image Width: " + webcamGrabber.getImageWidth());
        log.debug("Image Height: " + webcamGrabber.getImageHeight());
        log.debug("Frame Rate: " + frameRate);
    }

    /**
     * Print to terminal the supported audio formats.
     *
     * @param lineInfo the line info
     */
    private void printSupportedAudioFormats(final Line.Info lineInfo) {
        if (lineInfo instanceof final DataLine.Info dataLineInfo) {
            log.debug("Supported Audio Formats:");
            Arrays.stream(dataLineInfo.getFormats()).forEach(format -> log.debug("{}", format));
        }
    }
}
