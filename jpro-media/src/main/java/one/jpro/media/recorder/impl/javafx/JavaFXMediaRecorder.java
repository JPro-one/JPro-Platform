package one.jpro.media.recorder.impl.javafx;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import one.jpro.media.recorder.MediaRecorder;
import one.jpro.media.recorder.event.MediaRecorderEvent;
import one.jpro.media.recorder.impl.BaseMediaRecorder;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.*;

/**
 * {@link MediaRecorder} implementation for the desktop.
 *
 * @author Besmir Beqiri
 */
public final class JavaFXMediaRecorder extends BaseMediaRecorder {

    private final Logger log = LoggerFactory.getLogger(JavaFXMediaRecorder.class);

    private final ThreadGroup scheduledThreadGroup = new ThreadGroup("Media Recorder thread pool");
    private int threadCounter;
    private final ThreadFactory threadFactory = run -> {
        final Thread thread = new Thread(scheduledThreadGroup, run);
        thread.setName("Media Recorder Thread " + threadCounter++);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        return thread;
    };

    private static final int WEBCAM_DEVICE_INDEX = 0;
    private static final String MP4_FILE_EXTENSION = ".mp4";

    private ExecutorService videoExecutorService;
    private ScheduledExecutorService audioExecutorService;

    private final Stage stage;

    // Video resources
    private final OpenCVFrameGrabber webcamGrabber;
    private final JavaFXFrameConverter frameConverter;
    private final ImageView frameView;
    private final Pane cameraView;

    // Audio resources
    private static final int DEFAULT_AUDIO_SAMPLE_RATE = 44100; // 44.1 KHz
    private static final int DEFAULT_AUDIO_CHANNELS = 0; // no audio
    private AudioFormat audioFormat;
    private TargetDataLine micLine;
    private int audioSampleRate;
    private int audioNumChannels;

    // Storage resources
    private FFmpegFrameRecorder recorder;
    private Path tempVideoFile;
    private long startRecordingTime = 0;

    private volatile boolean cameraEnabled = false;
    private volatile boolean recordingStarted = false;

    public JavaFXMediaRecorder(Stage stage) {
        this.stage = stage;
        // OpenCV webcam frame grabber
        webcamGrabber = new OpenCVFrameGrabber(WEBCAM_DEVICE_INDEX);
        // Frame to JavaFX image converter
        frameConverter = new JavaFXFrameConverter();
        // Use ImageView to show camera grabbed frames
        frameView = new ImageView();
//        frameView.setPreserveRatio(true);
        cameraView = new Pane(frameView);
        frameView.fitWidthProperty().bind(cameraView.widthProperty());
        frameView.fitHeightProperty().bind(cameraView.heightProperty());

        // Stop and release native resources on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            release();
        }));
    }

    public Region getCameraView() {
        return cameraView;
    }

    @Override
    public void enable() {
        // Camera frame grabber runnable
        final Runnable frameGrabber = () -> {
            try {
                // start the video capture
                webcamGrabber.start();
                cameraEnabled = true;

                // start the audio capture
                enableAudioCapture();

                while (cameraEnabled) {
                    // effectively grab and process a single frame
                    final Frame frame = webcamGrabber.grab();
                    // convert and show the frame
                    updateCameraView(frameView, frameConverter.convert(frame));

                    if (recordingStarted) {
                        // write the webcam frame
                        writeVideoFrame(frame);
                    }
                }
            } catch (FrameGrabber.Exception ex) {
                log.error(ex.getMessage(), ex);
                release();
            } catch (LineUnavailableException lue) {
                log.error(lue.getMessage(), lue);
                if (micLine != null) {
                    micLine.close();
                }
            }
        };

        videoExecutorService = Executors.newSingleThreadExecutor(threadFactory);
        videoExecutorService.execute(frameGrabber);
    }

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

//                    log.info("Record audio samples: {}", nSamplesRead);

                    // recorder audio data
                    try {
                        recorder.recordSamples(audioSampleRate, audioNumChannels, samplesBuff);
                    } catch (FFmpegFrameRecorder.Exception ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            };

            final long period = (long) (1000.0 / webcamGrabber.getFrameRate());
            audioExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
            audioExecutorService.scheduleAtFixedRate(audioSampleGrabber, 0, period, TimeUnit.MILLISECONDS);
        }
    }

    private Mixer.Info getDefaultMicDevice() {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            Mixer m = AudioSystem.getMixer(info);
            Line.Info[] lineInfos = m.getTargetLineInfo();
            // Only prints out info is it is a Microphone
            if (lineInfos.length >= 1 && lineInfos[0].getLineClass().equals(TargetDataLine.class)) {
                for (Line.Info lineInfo : lineInfos) {
                    System.out.println("Mic Line Name: " + info.getName()); // The audio device name
                    System.out.println("Mic Line Description: " + info.getDescription()); // The type of audio device
                    showLineInfoFormats(lineInfo);
                }
                return info;
            }
        }
        return null;
    }

    private void showLineInfoFormats(final Line.Info lineInfo) {
        if (lineInfo instanceof DataLine.Info) {
            final DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
            System.out.println("Supported Audio Formats:");
            Arrays.stream(dataLineInfo.getFormats()).forEach(format -> System.out.println("    " + format));
        }
    }

    @Override
    public void start() {
        if (cameraEnabled) {
            tempVideoFile = createTempFilename("video_", MP4_FILE_EXTENSION);

            recorder = new FFmpegFrameRecorder(tempVideoFile.toString(),
                    webcamGrabber.getImageWidth(), webcamGrabber.getImageHeight());
            recorder.setInterleaved(true);
            recorder.setVideoOption("tune", "zerolatency");
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("crf", "28");
            recorder.setVideoBitrate(2000000); // 2000 kb/s, reasonable ok for 720
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setFrameRate(webcamGrabber.getFrameRate());
            recorder.setGopSize((int) (webcamGrabber.getFrameRate() * 2));
            recorder.setAudioOption("crf", "0"); // no variable bitrate audio
            recorder.setAudioQuality(0); // highest quality
            recorder.setAudioBitrate(192000); // 192 Kbps
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorder.setSampleRate(getAudioSampleRate());
            recorder.setAudioChannels(getAudioChannels());

            try {
                recorder.start();
            } catch (FFmpegFrameRecorder.Exception ex) {
                log.error(ex.getMessage(), ex);
            }

            // enable recording
            startRecordingTime = 0; // reset start recording time
            recordingStarted = true;

            // Set state
            setState(State.RECORDING);

            // Fire start event
            Event.fireEvent(JavaFXMediaRecorder.this,
                    new MediaRecorderEvent(JavaFXMediaRecorder.this,
                            MediaRecorderEvent.MEDIA_RECORDER_START));
        } else {
            log.info("Please, enable the camera first!");
        }
    }

    @Override
    public void pause() {
        // disable recording
        recordingStarted = false;

        // Set state
        setState(State.PAUSED);

        // Fire start event
        Event.fireEvent(JavaFXMediaRecorder.this,
                new MediaRecorderEvent(JavaFXMediaRecorder.this,
                        MediaRecorderEvent.MEDIA_RECORDER_PAUSE));
    }

    @Override
    public void resume() {
        // enable recording
        startRecordingTime = 0; // reset start recording time
        recordingStarted = true;

        // Set state
        setState(State.RECORDING);

        // Fire start event
        Event.fireEvent(JavaFXMediaRecorder.this,
                new MediaRecorderEvent(JavaFXMediaRecorder.this,
                        MediaRecorderEvent.MEDIA_RECORDER_RESUME));
    }

    @Override
    public void stop() {
        // disable recording
        recordingStarted = false;

        // Release video writer
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.close();
            }
        } catch (FrameRecorder.Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        // Set state
        setState(State.INACTIVE);

        // Fire start event
        Event.fireEvent(JavaFXMediaRecorder.this,
                new MediaRecorderEvent(JavaFXMediaRecorder.this,
                        MediaRecorderEvent.MEDIA_RECORDER_STOP));
    }

    @Override
    public void retrieve() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As...");
        fileChooser.setInitialFileName(tempVideoFile.getFileName() + MP4_FILE_EXTENSION);
        // Show save dialog
        final File saveToFile = fileChooser.showSaveDialog(stage);
        if (saveToFile != null) {
            try {
                Files.copy(tempVideoFile, saveToFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    private void writeVideoFrame(Frame frame) {
        try {
            if (startRecordingTime == 0) startRecordingTime = System.currentTimeMillis();

            // Create timestamp for this frame
            final long videoTimeStamp = 1000 * (System.currentTimeMillis() - startRecordingTime);

            // Check for AV drift
            if (videoTimeStamp > recorder.getTimestamp()) {
                log.info("AV drift correction: {} : {} -> {}",
                        videoTimeStamp, recorder.getTimestamp(), (videoTimeStamp - recorder.getTimestamp()));

                // Tell the recorder to write this frame at this timestamp
                recorder.setTimestamp(videoTimeStamp);
            }

            if (recorder != null) {
                recorder.record(frame);
            }
        } catch (FFmpegFrameRecorder.Exception ex) {
            log.info(ex.getMessage(), ex);
        }
    }

    /**
     * Stop the acquisition from the camera and microphone and release all the resources.
     */
    public void release() {
        // release video resources
        if (videoExecutorService != null && !videoExecutorService.isShutdown()) {
            cameraEnabled = false;

            try {
                // release video grabber
                if (webcamGrabber != null) {
                    webcamGrabber.release();
                }

                // stop the video recoding service
                videoExecutorService.shutdown();
                //noinspection ResultOfMethodCallIgnored
                videoExecutorService.awaitTermination(10, TimeUnit.MILLISECONDS);
            } catch (FrameGrabber.Exception | InterruptedException ex) {
                log.error("Exception in stopping the video frame capture service, trying to release the camera now... ", ex);
            }
        }

        // release audio resources
        if (audioExecutorService != null && !audioExecutorService.isShutdown()) {
            try {
                // release audio grabber
                if (micLine != null) {
                    micLine.close();
                }

                // stop the audio recording service
                audioExecutorService.shutdown();
                //noinspection ResultOfMethodCallIgnored
                audioExecutorService.awaitTermination(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                // log any exception
                log.error("Exception in stopping the frame capture, trying to release the camera now... ", ex);
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

        final Path tempFile = Path.of(System.getProperty("user.home"),
                ".jpro", "video", "capture", filename);
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
}
