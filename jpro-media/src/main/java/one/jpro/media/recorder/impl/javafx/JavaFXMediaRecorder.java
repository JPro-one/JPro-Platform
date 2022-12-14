package one.jpro.media.recorder.impl.javafx;

import javafx.event.Event;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import nu.pattern.OpenCV;
import one.jpro.media.recorder.MediaRecorder;
import one.jpro.media.recorder.event.MediaRecorderEvent;
import one.jpro.media.recorder.impl.BaseMediaRecorder;
import one.jpro.media.util.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@link MediaRecorder} implementation for the desktop.
 *
 * @author Besmir Beqiri
 */
public final class JavaFXMediaRecorder extends BaseMediaRecorder {

    static {
        // Load the OpenCV library
        OpenCV.loadLocally();
    }

    private final Logger log = LoggerFactory.getLogger(JavaFXMediaRecorder.class);

    private final ThreadGroup scheduledThreadGroup = new ThreadGroup("JavaFX Media Recorder thread pool");
    private final ThreadFactory scheduledThreadFactory = run -> {
        final Thread thread = new Thread(scheduledThreadGroup, run);
        thread.setName("JavaFX Media Recorder Thread");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        return thread;
    };

    private final static int WEBCAM_DEVICE_INDEX = 0;
    private final static int FRAME_RATE = 30;

    private ScheduledExecutorService captureTimer;
    private final VideoCapture videoCapture;
    private final ImageView frameView;
    private final Pane cameraView;
    private boolean cameraActive;
    private VideoWriter videoWriter;
    private Path tempVideoFile;

    private volatile boolean startRecord = false;

    // Locking mechanism
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

    public JavaFXMediaRecorder() {
        // the OpenCV object that realizes the video capture
        videoCapture = new VideoCapture();

        // Use ImageView to show camera grabbed frames
        frameView = new ImageView();
//        frameView.setPreserveRatio(true);
        cameraView = new Pane(frameView);
        frameView.fitWidthProperty().bind(cameraView.widthProperty());
        frameView.fitHeightProperty().bind(cameraView.heightProperty());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopAcquisition();
            stop();
        }));
    }

    public Region getCameraView() {
        return cameraView;
    }

    @Override
    public void enable() {
        if (!cameraActive) {
            // start the video capture
            videoCapture.open(WEBCAM_DEVICE_INDEX);

            // is the video stream available?
            if (videoCapture.isOpened()) {
                cameraActive = true;

                // grab a frame
                final Runnable frameGrabber = () -> {
                    // effectively grab and process a single frame
                    final Mat frame = readFrame();
                    // convert and show the frame
                    Image cameraImage = Utils.mat2Image(frame);
                    updateCameraView(frameView, cameraImage);

                    if (startRecord) {
                        // write the frame
                        writeFrame(frame);
                    }
                };

                captureTimer = Executors.newScheduledThreadPool(2, scheduledThreadFactory);
                captureTimer.scheduleAtFixedRate(frameGrabber, 0, 1000 / FRAME_RATE, TimeUnit.MILLISECONDS);
            } else {
                log.error("Impossible to open the camera connection...");
            }
        } else {
            // the camera is not active at this point
            cameraActive = false;
            // stop the timer
            stopAcquisition();
        }
    }

    @Override
    public void start() {
        int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G'); // MJPG format

        double fps = videoCapture.get(Videoio.CAP_PROP_FPS);
        final Size size = new Size((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        tempVideoFile = createTempFilename("video_", ".mp4");
        videoWriter = new VideoWriter(tempVideoFile.toString(), fourcc, fps, size, true);
        log.info("Video writer is open: {} at {}", videoWriter.isOpened(), tempVideoFile.toString());

        // enable recording
        startRecord = true;

        // Set state
        setState(State.RECORDING);

        // Fire start event
        Event.fireEvent(JavaFXMediaRecorder.this,
                new MediaRecorderEvent(JavaFXMediaRecorder.this,
                        MediaRecorderEvent.MEDIA_RECORDER_START));
    }

    @Override
    public void pause() {
        // disable recording
        startRecord = false;

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
        startRecord = true;

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
        startRecord = false;

        // Release video writer
        writeLock.lock();
        try {
            if (videoWriter != null) {
                videoWriter.release();
            }
        } finally {
            writeLock.unlock();
        }

        // Set state
        setState(State.INACTIVE);

        // Fire start event
        Event.fireEvent(JavaFXMediaRecorder.this,
                new MediaRecorderEvent(JavaFXMediaRecorder.this,
                        MediaRecorderEvent.MEDIA_RECORDER_STOP));
    }

    @Override
    public void download() {

    }

    /**
     * Get a frame from the opened camera/video stream.
     *
     * @return the {@link Image} to show
     */
    private Mat readFrame() {
        readLock.lock();

        try {
            Mat frame = new Mat();

            // check if the capture is open
            if (videoCapture.isOpened()) {
                try {
                    // read the current frame
                    videoCapture.read(frame);
                } catch (Exception ex) {
                    // log the error
                    log.error("Exception during the frame elaboration: ", ex);
                }
            }

            return frame;
        } finally {
            readLock.unlock();
        }
    }

    private void writeFrame(Mat frame) {
        writeLock.lock();

        try {
            if (videoWriter != null && videoWriter.isOpened()) {
                videoWriter.write(frame);
                log.info("Current write thread: {}", Thread.currentThread().getName());
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Stop the acquisition from the camera and release all the resources.
     */
    private void stopAcquisition() {
        if (captureTimer != null && !captureTimer.isShutdown()) {
            try {
                // stop the timer
                captureTimer.shutdown();
                //noinspection ResultOfMethodCallIgnored
                captureTimer.awaitTermination(1000 / FRAME_RATE, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                // log any exception
                log.error("Exception in stopping the frame capture, trying to release the camera now... ", ex);
            }
        }

        if (videoCapture.isOpened()) {
            // release the camera
            videoCapture.release();
        }
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view  the {@link ImageView} to update
     * @param image the {@link Image} to show
     */
    private void updateCameraView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * Create a random filename with the given prefix and postfix.
     *
     * @param prefix  the filename prefix
     * @param postfix the filename postfix
     * @return a string containing the entire path with the created filename
     */
    private Path createTempFilename(String prefix, String postfix) {
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
