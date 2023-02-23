package one.jpro.media;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.media.recorder.MediaRecorder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static one.jpro.media.recorder.MediaRecorder.Status;
import static org.testfx.assertions.api.Assertions.assertThat;

/**
 * Automated tests for {@link MediaRecorder} API.
 *
 * @author Besmir Beqiri
 */
@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MediaRecorderTests {

    private final Logger log = LoggerFactory.getLogger(MediaRecorderTests.class);

    private MediaRecorder mediaRecorder;
    private MediaView cameraView;
    private Button enableCamButton;
    private Button startButton;
    private Button pauseButton;
    private Button stopButton;

    @Start
    public void start(Stage stage) {
        // Create media recorder and media view
        mediaRecorder = MediaRecorder.create(stage);
        cameraView = MediaView.create(mediaRecorder);

        // Controls
        enableCamButton = new Button("Enable Cam");
        startButton = new Button("Start");
        startButton.setDisable(true);
        pauseButton = new Button("Pause");
        pauseButton.setDisable(true);
        stopButton = new Button("Stop");
        stopButton.setDisable(true);

        StackPane previewPane = new StackPane(enableCamButton);

        FlowPane controlsPane = new FlowPane(startButton, pauseButton, stopButton);
        controlsPane.setAlignment(Pos.BOTTOM_CENTER);
        controlsPane.setHgap(8);
        controlsPane.setVgap(8);
        controlsPane.setPadding(new Insets(8));

        // Control events
        enableCamButton.setOnAction(event -> mediaRecorder.enable());
        startButton.setOnAction(event -> mediaRecorder.start());
        pauseButton.setOnAction(event -> mediaRecorder.pause());
        stopButton.setOnAction(event -> mediaRecorder.stop());

        // Event handlers
        mediaRecorder.setOnReady(event -> {
            startButton.setDisable(false);
            previewPane.getChildren().setAll(cameraView);
        });
        mediaRecorder.setOnStart(event -> {
            startButton.setDisable(true);
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
        });
        mediaRecorder.setOnPause(event -> {
            startButton.setDisable(false);
            pauseButton.setDisable(true);
        });
        mediaRecorder.setOnResume(event -> {
            startButton.setDisable(true);
            pauseButton.setDisable(false);
        });
        mediaRecorder.setOnStopped(event -> {
            startButton.setDisable(false);
            pauseButton.setDisable(true);
            stopButton.setDisable(true);
        });
        mediaRecorder.setOnError(event -> System.out.println(mediaRecorder.getError().toString()));

        // User interface
        VBox rootPane = new VBox(previewPane, controlsPane);
        VBox.setVgrow(previewPane, Priority.ALWAYS);
        cameraView.fitWidthProperty().bind(rootPane.widthProperty());
        cameraView.fitHeightProperty().bind(rootPane.heightProperty()
                .subtract(controlsPane.heightProperty()));

        Scene scene = new Scene(rootPane, 760, 540);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @Order(1)
    public void media_recorder_enable(FxRobot robot) throws TimeoutException {
        log.debug("MediaRecorder => Testing enable functionality...");
        log.debug("Click on enable camera button");
        robot.clickOn(enableCamButton); // Enable camera (asynchronous operation)

        log.debug("Wait for media recorder to be ready...");
        waitForStatus(Status.READY, 10, TimeUnit.SECONDS);
        log.debug("Media recorder is ready");

        log.debug("Run additional checks...");
        assertThat(startButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isTrue();
        assertThat(stopButton.isDisable()).isTrue();
        log.debug("Checks passed");
        log.debug("MediaRecorder => Enable test successfully passed.");
    }

    @Test
    @Order(2)
    public void media_recorder_controls(FxRobot robot) throws TimeoutException {
        log.debug("MediaRecorder => Testing controls...");
        log.debug("Click on enable camera button");
        robot.clickOn(enableCamButton); // Enable camera (asynchronous operation)
        log.debug("Wait for media recorder to be ready...");
        waitForStatus(Status.READY, 10, TimeUnit.SECONDS);
        log.debug("Media recorder is ready");
        log.debug("Run additional checks...");
        assertThat(startButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isTrue();
        assertThat(stopButton.isDisable()).isTrue();
        log.debug("Checks passed");

        log.debug("Click on start button");
        robot.clickOn(startButton); // Start recording (asynchronous operation)
        log.debug("Wait for media recorder to start...");
        waitForStatus(Status.RECORDING);
        log.debug("Media recorder has started recording");
        log.debug("Run additional checks...");
        assertThat(startButton.isDisable()).isTrue();
        assertThat(pauseButton.isDisable()).isFalse();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");
        log.debug("Wait for media recorder to record for at least 5 seconds...");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaRecorder.getDuration().greaterThan(Duration.seconds(5)),
                mediaRecorder.durationProperty()));

        log.debug("Click on pause button");
        robot.clickOn(pauseButton); // Pause recording (asynchronous operation)
        log.debug("Run additional checks...");
        assertThat(mediaRecorder.getStatus()).isEqualByComparingTo(Status.PAUSED);
        assertThat(startButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isTrue();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");

        log.debug("Click on start button to resume button");
        robot.clickOn(startButton); // Resume recording (asynchronous operation)
        log.debug("Run additional checks...");
        assertThat(mediaRecorder.getStatus()).isEqualByComparingTo(Status.RECORDING);
        assertThat(startButton.isDisable()).isTrue();
        assertThat(pauseButton.isDisable()).isFalse();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");

        log.debug("Click on stop button");
        robot.clickOn(stopButton); // Stop recording (asynchronous operation)
        log.debug("Wait for media recorder to stop...");
        waitForStatus(Status.INACTIVE);
        log.debug("Media recorder has stop recording");
        log.debug("Run additional checks...");
        assertThat(startButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isTrue();
        assertThat(stopButton.isDisable()).isTrue();
        log.debug("Checks passed");
        log.debug("MediaRecorder => Test successfully passed.");
    }

    @Test
    @Order(3)
    public void media_recorder_stress(FxRobot robot) throws TimeoutException {
        log.debug("MediaRecorder => Stress test started...");
        log.debug("Click on enable camera button");
        robot.clickOn(enableCamButton); // Enable camera (asynchronous operation)
        log.debug("Wait for media recorder to be ready...");
        waitForStatus(Status.READY, 10, TimeUnit.SECONDS);
        log.debug("Media recorder is ready");
        log.debug("Run additional checks...");
        assertThat(startButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isTrue();
        assertThat(stopButton.isDisable()).isTrue();
        log.debug("Checks passed");

        // Stress test for 10 times
        for (int i = 0; i < 10; i++) {
            log.debug("Click on start button");
            robot.clickOn(startButton); // Start recording (asynchronous operation)
            log.debug("Wait for media recorder to start...");
            waitForStatus(Status.RECORDING);
            log.debug("Media recorder has started recording");
            log.debug("Run additional checks...");
            assertThat(startButton.isDisable()).isTrue();
            assertThat(pauseButton.isDisable()).isFalse();
            assertThat(stopButton.isDisable()).isFalse();
            log.debug("Checks passed");
            log.debug("Wait for media recorder to record for at least 2 seconds...");
            WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                            mediaRecorder.getDuration().greaterThan(Duration.seconds(2)),
                    mediaRecorder.durationProperty()));
            
            if (i % 2 == 0) {
                log.debug("Click on pause button");
                robot.clickOn(pauseButton); // Pause recording (asynchronous operation)
                log.debug("Wait for media recorder to pause...");
                waitForStatus(Status.PAUSED);
                log.debug("Media recorder has paused");
                log.debug("Run additional checks...");
                assertThat(startButton.isDisable()).isFalse();
                assertThat(pauseButton.isDisable()).isTrue();
                assertThat(stopButton.isDisable()).isFalse();
                log.debug("Checks passed");
            }

            log.debug("Click on stop button");
            robot.clickOn(stopButton); // Stop recording (asynchronous operation)
            waitForStatus(Status.INACTIVE);
            log.debug("Media recorder has stop recording");
            log.debug("Run additional checks...");
            assertThat(startButton.isDisable()).isFalse();
            assertThat(pauseButton.isDisable()).isTrue();
            assertThat(stopButton.isDisable()).isTrue();
            log.debug("Checks passed");
        }
        log.debug("MediaRecorder => Stress test successfully passed.");
    }

    private void waitForStatus(Status status) throws TimeoutException {
        waitForStatus(status, 5, TimeUnit.SECONDS);
    }

    private void waitForStatus(Status status, long timeout, TimeUnit timeUnit) throws TimeoutException {
        WaitForAsyncUtils.waitFor(timeout, timeUnit, mediaRecorder.statusProperty().isEqualTo(status));
        assertThat(mediaRecorder.getStatus()).isEqualByComparingTo(status);
        WaitForAsyncUtils.waitForFxEvents();
    }
}
