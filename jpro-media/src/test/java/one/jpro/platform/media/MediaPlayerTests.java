package one.jpro.platform.media;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.platform.media.player.MediaPlayer;
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

import static javafx.scene.media.MediaPlayer.Status;
import static org.testfx.assertions.api.Assertions.assertThat;

/**
 * Automated tests for {@link MediaPlayer} API.
 *
 * @author Besmir Beqiri
 */
@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MediaPlayerTests {

    private final Logger log = LoggerFactory.getLogger(MediaPlayerTests.class);

    private static final String MEDIA_SOURCE = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Slider seekSlider;
    private CheckBox preserveRatioCheckBox;
    private CheckBox muteCheckBox;
    private volatile boolean eom = false;
    private Scene scene;

    @Start
    private void start(Stage stage) {
        // Provide a media source
        final MediaSource mediaSource = new MediaSource(MEDIA_SOURCE);

        // Create the media player and the media view.
        mediaPlayer = MediaPlayer.create(stage, mediaSource);
        mediaView = MediaView.create(mediaPlayer);
        mediaView.setPreserveRatio(false);

        // Media controls
        playButton = new Button("Play");
        playButton.setDisable(true);
        pauseButton = new Button("Pause");
        pauseButton.setDisable(true);
        stopButton = new Button("Stop");
        stopButton.setDisable(true);
        seekSlider = new Slider();
        seekSlider.setId("seekSlider");
        seekSlider.setPrefWidth(440);
        preserveRatioCheckBox = new CheckBox("Preserve Ratio");
        preserveRatioCheckBox.setSelected(mediaView.isPreserveRatio());
        muteCheckBox = new CheckBox("Mute");
        muteCheckBox.setSelected(mediaPlayer.isMute());

        // Control events
        playButton.setOnAction(event -> mediaPlayer.play());
        pauseButton.setOnAction(event -> mediaPlayer.pause());
        stopButton.setOnAction(event -> mediaPlayer.stop());
        seekSlider.setOnMousePressed(mouseEvent ->
                mediaPlayer.seek(Duration.seconds(seekSlider.getValue())));
        seekSlider.setOnMouseDragged(mouseEvent ->
                mediaPlayer.seek(Duration.seconds(seekSlider.getValue())));
        mediaPlayer.currentTimeProperty().addListener(observable -> {
            if (mediaPlayer.getDuration().greaterThan(Duration.ZERO)
                    && !seekSlider.isDisabled()
                    && !seekSlider.isValueChanging()
                    && !seekSlider.isPressed()) {
                seekSlider.setValue(mediaPlayer.getCurrentTime().toSeconds());
            }
        });
        preserveRatioCheckBox.setOnAction(event ->
                mediaView.setPreserveRatio(preserveRatioCheckBox.isSelected()));

        muteCheckBox.setOnAction(event -> mediaPlayer.setMute(muteCheckBox.isSelected()));

        // Event handlers
        mediaPlayer.setOnReady(event -> {
            playButton.setDisable(false);
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
            seekSlider.setMax(mediaPlayer.getDuration().toSeconds());
        });
        mediaPlayer.setOnPlaying(event -> {
            playButton.setDisable(true);
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
        });
        mediaPlayer.setOnPaused(event -> {
            playButton.setDisable(false);
            pauseButton.setDisable(true);
            stopButton.setDisable(false);
        });
        mediaPlayer.setOnStopped(event -> {
            playButton.setDisable(false);
            pauseButton.setDisable(true);
            stopButton.setDisable(true);
        });
        mediaPlayer.setOnEndOfMedia(event -> {
            eom = true;
            System.out.println("End of media reached");
        });
        mediaPlayer.setOnError(event -> System.out.println(mediaPlayer.getError().toString()));

        mediaPlayer.currentRateProperty().addListener(observable -> {
            log.debug("mediaPlayer.currentRate(): {}", mediaPlayer.getCurrentRate());
        });

        // User interface
        FlowPane controlsPane = new FlowPane(playButton, pauseButton, stopButton, seekSlider,
                preserveRatioCheckBox, muteCheckBox);
        controlsPane.setAlignment(Pos.BOTTOM_CENTER);
        controlsPane.setHgap(8);
        controlsPane.setVgap(8);
        controlsPane.setPadding(new Insets(8));
        StackPane rootPane = new StackPane(mediaView, controlsPane);

        scene = new Scene(rootPane, 800, 540);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @Order(1)
    void media_player_controls(FxRobot robot) throws TimeoutException {
        log.debug("MediaPlayer => Testing controls...");
        waitForStatusReady();

        clickPlayButton(robot);
        log.debug("Wait for media player to play for at least 5 seconds");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(5)),
                mediaPlayer.currentTimeProperty()));

        log.debug("Click pause button");
        robot.clickOn(pauseButton); // Pause media (asynchronous operation)
        waitForStatus(Status.PAUSED);
        log.debug("Run additional checks...");
        assertThat(playButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isTrue();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");

        log.debug("Click play button");
        robot.clickOn(playButton);

        log.debug("Click on preserve ratio check box");
        robot.clickOn(preserveRatioCheckBox);
        log.debug("Run checks...");
        assertThat(mediaView.isPreserveRatio()).isTrue();
        log.debug("Checks passed");

        log.debug("Waiting for media player to play for additional 3 seconds...");
        final Duration currentTime1 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime1.add(Duration.seconds(3))),
                mediaPlayer.currentTimeProperty()));

        log.debug("Click on preserve ratio check box again");
        robot.clickOn(preserveRatioCheckBox);
        log.debug("Run checks...");
        assertThat(mediaView.isPreserveRatio()).isFalse();
        log.debug("Checks passed");

        log.debug("Waiting for media player to play for additional 3 seconds...");
        final Duration currentTime2 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime2.add(Duration.seconds(3))),
                mediaPlayer.currentTimeProperty()));

        log.debug("Click on mute check box");
        robot.clickOn(muteCheckBox);
        log.debug("Run checks...");
        assertThat(muteCheckBox.isSelected()).isTrue();
        assertThat(mediaPlayer.isMute()).isTrue();
        log.debug("Checks passed");

        log.debug("Waiting for media player to play for additional 3 seconds...");
        final Duration currentTime3 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime3.add(Duration.seconds(3))),
                mediaPlayer.currentTimeProperty()));

        log.debug("Click on mute check box again");
        robot.clickOn(muteCheckBox);
        log.debug("Run checks...");
        assertThat(muteCheckBox.isSelected()).isFalse();
        assertThat(mediaPlayer.isMute()).isFalse();
        log.debug("Checks passed");

        log.debug("Waiting for media player to play for additional 3 seconds...");
        final Duration currentTime4 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime4.add(Duration.seconds(3))),
                mediaPlayer.currentTimeProperty()));

        clickStopButton(robot);
        log.debug("MediaPlayer => Test successfully passed.");
    }

    @Test
    @Order(2)
    void seek_after_media_player_is_ready(FxRobot robot) throws TimeoutException {
        log.debug("MediaPlayer => Testing seek after media player is ready...");
        waitForStatusReady();

        final Duration seekTime = Duration.seconds(120);
        log.debug("Seek to {} seconds", seekTime.toSeconds());
        mediaPlayer.seek(seekTime);
        WaitForAsyncUtils.waitForFxEvents();
        log.debug("Check current time is {} seconds", seekTime.toSeconds());
        assertThat(mediaPlayer.getCurrentTime()).isEqualByComparingTo(seekTime);
        log.debug("Run additional checks...");
        assertThat(playButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isFalse();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");

        clickPlayButton(robot);
        assertThat(mediaPlayer.getCurrentTime()).isGreaterThanOrEqualTo(seekTime);

        log.debug("Wait for media player to play for additional 3 seconds");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(seekTime.add(Duration.seconds(3))),
                mediaPlayer.currentTimeProperty()));

        clickStopButton(robot);
        log.debug("MediaPlayer => Test successfully passed.");
    }

    @Test
    @Order(3)
    void seek_after_media_player_is_playing(FxRobot robot) throws TimeoutException {
        log.debug("MediaPlayer => Testing seek after media player is playing");
        waitForStatusReady();

        clickPlayButton(robot);

        log.debug("Seek to 360 seconds by clicking on seek slider");
        final Duration seekTime = Duration.seconds(360);
        seekViaRobot(robot, seekTime);
        log.debug("Check current time is greater or equal to 360 seconds");
        assertThat(mediaPlayer.getCurrentTime()).isGreaterThanOrEqualTo(seekTime);
        log.debug("Run additional checks...");
        assertThat(playButton.isDisable()).isTrue();
        assertThat(pauseButton.isDisable()).isFalse();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");

        log.debug("Wait for media player to play for additional 4 seconds");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(seekTime.add(Duration.seconds(4))),
                mediaPlayer.currentTimeProperty()));

        clickStopButton(robot);
        log.debug("MediaPlayer => Test successfully passed.");
    }

    @Test
    @Order(4)
    void seek_after_media_player_is_paused(FxRobot robot) throws TimeoutException {
        log.debug("MediaPlayer => Testing seek after media player is paused...");
        waitForStatusReady();

        log.debug("Click on play button");
        robot.clickOn(playButton);
        log.debug("Click on pause button");
        robot.clickOn(pauseButton);

        log.debug("Seek to exactly 483 seconds");
        final Duration seekTime = Duration.seconds(483);
        mediaPlayer.seek(seekTime);
        WaitForAsyncUtils.waitForFxEvents();
        log.debug("Check current time is 483 seconds");
        assertThat(mediaPlayer.getCurrentTime()).isEqualTo(Duration.seconds(483));
        log.debug("Run additional checks...");
        assertThat(playButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isTrue();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");

        clickPlayButton(robot);
        log.debug("Waiting for media player to play for additional 5.9 seconds");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(seekTime.add(Duration.seconds(5.9))),
                mediaPlayer.currentTimeProperty()));

        clickStopButton(robot);
        log.debug("MediaPlayer => Test successfully passed.");
    }

    @Test
    @Order(5)
    void seek_after_media_player_reached_eom(FxRobot robot) throws TimeoutException, InterruptedException {
        log.debug("MediaPlayer => Testing seek after media player has reached EOM...");
        waitForStatusReady();

        clickPlayButton(robot);

        final Duration seekTime = mediaPlayer.getDuration().subtract(Duration.seconds(2));
        log.debug("Seek to {} seconds", seekTime.toSeconds());
        mediaPlayer.seek(seekTime);
        WaitForAsyncUtils.waitForFxEvents();
        log.debug("Check current time is {} seconds", seekTime.toSeconds());
        assertThat(mediaPlayer.getCurrentTime()).isEqualByComparingTo(seekTime);
        log.debug("Run additional checks...");
        assertThat(playButton.isDisable()).isTrue();
        assertThat(pauseButton.isDisable()).isFalse();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, () -> eom);
        waitForStatus(Status.PAUSED);
        log.debug("Run additional checks...");
        assertThat(playButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isTrue();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");

        final Duration seekTime2 = Duration.seconds(18);
        log.debug("Seek to {} seconds", seekTime2.toSeconds());
        mediaPlayer.seek(seekTime2);
        WaitForAsyncUtils.waitForFxEvents();
        log.debug("Check current time is {} seconds", seekTime2.toSeconds());
        assertThat(mediaPlayer.getCurrentTime()).isEqualByComparingTo(seekTime2);

        // wait for 1 second:
        TimeUnit.SECONDS.sleep(1);

        clickPlayButton(robot);
        log.debug("Wait for media player to play for additional 3 seconds");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(seekTime2.add(Duration.seconds(3))),
                mediaPlayer.currentTimeProperty()));

        clickStopButton(robot);
        log.debug("MediaPlayer => Test successfully passed.");
    }

    @Test
    @Order(6)
    void seek_to_negative_time(FxRobot robot) throws TimeoutException {
        log.debug("MediaPlayer => Testing seek to negative time...");
        waitForStatusReady();

        log.debug("Seek to -1 seconds");
        mediaPlayer.seek(Duration.seconds(-1));
        log.debug("Check current time is zero");
        assertThat(mediaPlayer.getCurrentTime()).isEqualByComparingTo(Duration.ZERO);
        log.debug("Run additional checks...");
        assertThat(mediaPlayer.getError()).isNotNull();
        assertThat(playButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isFalse();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");

        clickPlayButton(robot);
        log.debug("Waiting for media player to play for additional 3 seconds");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(3)),
                mediaPlayer.currentTimeProperty()));

        clickStopButton(robot);
        log.debug("MediaPlayer => Test successfully passed.");
    }

    @Test
    @Order(7)
    public void media_view_fit_width_height(FxRobot robot) throws TimeoutException {
        log.debug("MediaPlayer => Testing media view fit width and height...");
        waitForStatusReady();

        clickPlayButton(robot);

        log.debug("Waiting for media player to play for additional 2 seconds...");
        final Duration currentTime1 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime1.add(Duration.seconds(2))),
                mediaPlayer.currentTimeProperty()));

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(mediaView.fitWidthProperty(), scene.getWidth()),
                        new KeyValue(mediaView.fitHeightProperty(), scene.getHeight())),
                new KeyFrame(Duration.seconds(3.0),
                        new KeyValue(mediaView.fitWidthProperty(), 0.0),
                        new KeyValue(mediaView.fitHeightProperty(), 0.0)),
                new KeyFrame(Duration.seconds(6.0),
                        new KeyValue(mediaView.fitWidthProperty(), scene.getWidth()),
                        new KeyValue(mediaView.fitHeightProperty(), scene.getHeight())));
        timeline.play();

        log.debug("Waiting for media player to play for additional 6 seconds...");
        final Duration currentTime2 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime2.add(Duration.seconds(6))),
                mediaPlayer.currentTimeProperty()));

        log.debug("Reset media view fit width and height to -1.0");
        mediaView.setFitWidth(-1.0);
        assertThat(mediaView.getFitWidth()).isEqualTo(-1.0);
        mediaView.setFitHeight(-1.0);
        assertThat(mediaView.getFitHeight()).isEqualTo(-1.0);

        log.debug("Waiting for media player to play for additional 3 seconds...");
        final Duration currentTime3 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime3.add(Duration.seconds(3))),
                mediaPlayer.currentTimeProperty()));

        clickStopButton(robot);
        log.debug("MediaPlayer => Test successfully passed.");
    }

    @Test
    @Order(8)
    public void playback_rate_behaviour(FxRobot robot) throws TimeoutException, InterruptedException {
        log.debug("MediaPlayer => Testing media playback rate...");
        waitForStatusReady();

        log.debug("Check initial playback rate and current rate in ready state...");
        assertThat(mediaPlayer.getRate()).isEqualTo(1.0);
        assertThat(mediaPlayer.getCurrentRate()).isEqualTo(0.0);
        log.debug("Checks passed");

        robot.clickOn(playButton);

        log.debug("Waiting for media player to play for additional 5 seconds...");
        final Duration currentTime1 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime1.add(Duration.seconds(5))),
                mediaPlayer.currentTimeProperty()));

        log.debug("Check initial playback rate and current rate in playing state...");
        assertThat(mediaPlayer.getRate()).isEqualTo(1.0);
        assertThat(mediaPlayer.getCurrentRate()).isEqualTo(1.0);
        log.debug("Checks passed");

        robot.clickOn(pauseButton);

        log.debug("Check initial playback rate and current rate in paused state...");
        assertThat(mediaPlayer.getRate()).isEqualTo(1.0);
        assertThat(mediaPlayer.getCurrentRate()).isEqualTo(0.0);
        log.debug("Checks passed");

        log.debug("Set playback rate to 4.0");
        mediaPlayer.setRate(4.0);

        robot.clickOn(playButton);

        log.debug("Waiting for media player to play for additional 20 seconds...");
        final Duration currentTime2 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime2.add(Duration.seconds(20))),
                mediaPlayer.currentTimeProperty()));

        log.debug("Check initial playback rate and current rate in playing state...");
        assertThat(mediaPlayer.getRate()).isEqualTo(4.0);
        assertThat(mediaPlayer.getCurrentRate()).isEqualTo(4.0);
        log.debug("Checks passed");

        log.debug("Set playback rate to 2.0 while playing");
        mediaPlayer.setRate(2.0);

        log.debug("Waiting for media player to play for additional 10 seconds...");
        final Duration currentTime3 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime3.add(Duration.seconds(5))),
                mediaPlayer.currentTimeProperty()));
        WaitForAsyncUtils.waitForFxEvents();

        log.debug("Check new playback rate and current rate in playing state...");
        assertThat(mediaPlayer.getRate()).isEqualTo(2.0);
        assertThat(mediaPlayer.getCurrentRate()).isEqualTo(4.0);
        log.debug("Checks passed");

        log.debug("Set playback rate to -1.0 while playing");
        mediaPlayer.setRate(-1.0); // less than 0.0, is clamp to 0.0

        log.debug("Check new playback rate and current rate in playing state...");
        assertThat(mediaPlayer.getRate()).isEqualTo(-1.0);
        assertThat(mediaPlayer.getCurrentRate()).isEqualTo(4.0);
        log.debug("Checks passed");

        robot.clickOn(pauseButton);
        TimeUnit.SECONDS.sleep(1);
        log.debug("Set playback rate to 10.0");
        mediaPlayer.setRate(10.0); // more than 8.0, is clamp to 8.0
        robot.clickOn(playButton);

        log.debug("Waiting for media player to play for additional 30 seconds...");
        final Duration currentTime5 = mediaPlayer.getCurrentTime();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(currentTime5.add(Duration.seconds(30))),
                mediaPlayer.currentTimeProperty()));
        WaitForAsyncUtils.waitForFxEvents();

        log.debug("Check new playback rate and current rate in playing state...");
        assertThat(mediaPlayer.getRate()).isEqualTo(10.0);
        assertThat(mediaPlayer.getCurrentRate()).isEqualTo(10.0);
        log.debug("Checks passed");

        robot.clickOn(stopButton);

        log.debug("Check playback rate and current rate in stopped state...");
        assertThat(mediaPlayer.getRate()).isEqualTo(10.0);
        assertThat(mediaPlayer.getCurrentRate()).isEqualTo(0.0);
        log.debug("Checks passed");
    }

    private void waitForStatusReady() throws TimeoutException {
        waitForStatus(Status.READY);
        log.debug("Run additional checks...");
        assertThat(playButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isFalse();
        assertThat(stopButton.isDisable()).isFalse();
        assertThat(mediaView.getFitWidth()).isEqualTo(-1.0);
        assertThat(mediaView.getFitHeight()).isEqualTo(-1.0);
        log.debug("Checks passed");
    }

    private void clickPlayButton(FxRobot robot) throws TimeoutException {
        log.debug("Click on play button");
        robot.clickOn(playButton);
        waitForStatus(Status.PLAYING);
        log.debug("Run additional checks...");
        assertThat(playButton.isDisable()).isTrue();
        assertThat(pauseButton.isDisable()).isFalse();
        assertThat(stopButton.isDisable()).isFalse();
        log.debug("Checks passed");
    }

    private void clickStopButton(FxRobot robot) throws TimeoutException {
        log.debug("Click on stop button");
        robot.clickOn(stopButton);
        waitForStatus(Status.STOPPED);
        log.debug("Run additional checks...");
        assertThat(playButton.isDisable()).isFalse();
        assertThat(pauseButton.isDisable()).isTrue();
        assertThat(stopButton.isDisable()).isTrue();
        log.debug("Checks passed");
    }

    private void waitForStatus(Status status) throws TimeoutException {
        log.debug("Waiting for media player status {}...", status);
        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, mediaPlayer.statusProperty().isEqualTo(status));
        assertThat(mediaPlayer.getStatus()).isEqualByComparingTo(status);
        log.debug("Media player status is {}", status);
    }

    private void seekViaRobot(FxRobot robot, Duration duration) {
        double fracTime = duration.toMillis() / mediaPlayer.getDuration().toMillis();
        var seekThumb = robot.lookup("#seekSlider > .thumb").query();
        var seekThumbBounds = seekThumb.getLayoutBounds();
        robot.moveTo("#seekSlider > .thumb")
                .moveBy(fracTime * seekSlider.getWidth() - seekThumbBounds.getWidth() - seekThumb.getBoundsInLocal().getMinX(), 0)
                .press(MouseButton.PRIMARY)
                .release(MouseButton.PRIMARY);
    }
}
