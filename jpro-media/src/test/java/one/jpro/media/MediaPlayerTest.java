package one.jpro.media;

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
import one.jpro.media.player.MediaPlayer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static javafx.scene.media.MediaPlayer.Status;
import static org.testfx.assertions.api.Assertions.assertThat;

/**
 * Automated tests for MediaPlayer API.
 *
 * @author Besmir Beqiri
 */
@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MediaPlayerTest {

    private static final String MEDIA_SOURCE = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Slider seekSlider;
    private CheckBox preserveRatioCheckBox;
    private CheckBox muteCheckBox;

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
        mediaPlayer.setOnError(event -> System.out.println(mediaPlayer.getError().toString()));

        // User interface
        FlowPane controlsPane = new FlowPane(playButton, pauseButton, stopButton, seekSlider,
                preserveRatioCheckBox, muteCheckBox);
        controlsPane.setAlignment(Pos.BOTTOM_CENTER);
        controlsPane.setHgap(8);
        controlsPane.setVgap(8);
        controlsPane.setPadding(new Insets(8));
        StackPane rootPane = new StackPane(mediaView, controlsPane);

        Scene scene = new Scene(rootPane, 800, 540);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @Order(1)
    void control_buttons_play_pause_stop(FxRobot robot) throws TimeoutException {
        waitForStatus(Status.READY);

        // Play
        robot.clickOn(playButton);
        assertThat(mediaPlayer.getStatus()).isEqualByComparingTo(Status.PLAYING);
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(5)),
                mediaPlayer.currentTimeProperty()));

        // Pause
        robot.clickOn(pauseButton);
        assertThat(mediaPlayer.getStatus()).isEqualByComparingTo(Status.PAUSED);

        // Stop
        robot.clickOn(stopButton);
        assertThat(mediaPlayer.getStatus()).isEqualByComparingTo(Status.STOPPED);
    }

    @Test
    @Order(2)
    void media_view_is_preserve_ratio(FxRobot robot) throws TimeoutException {
        waitForStatus(Status.READY);

        // Play
        robot.clickOn(playButton);

        robot.clickOn(preserveRatioCheckBox);
        assertThat(mediaView.isPreserveRatio()).isTrue();

        // Play for 3 seconds
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(3)),
                mediaPlayer.currentTimeProperty()));

        robot.clickOn(preserveRatioCheckBox);
        assertThat(mediaView.isPreserveRatio()).isFalse();

        // Play for 3 seconds
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(6)),
                mediaPlayer.currentTimeProperty()));

        robot.clickOn(stopButton);
        assertThat(mediaPlayer.getStatus()).isEqualByComparingTo(Status.STOPPED);
    }

    @Test
    @Order(3)
    void media_player_is_muted(FxRobot robot) throws TimeoutException {
        waitForStatus(Status.READY);

        // Play
        robot.clickOn(playButton);

        robot.clickOn(muteCheckBox);
        assertThat(muteCheckBox.isSelected()).isTrue();
        assertThat(mediaPlayer.isMute()).isTrue();

        // Play for 3 seconds
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(3)),
                mediaPlayer.currentTimeProperty()));

        robot.clickOn(muteCheckBox);
        assertThat(muteCheckBox.isSelected()).isFalse();
        assertThat(mediaPlayer.isMute()).isFalse();

        // Play for 3 seconds
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(6)),
                mediaPlayer.currentTimeProperty()));

        robot.clickOn(stopButton);
        assertThat(mediaPlayer.getStatus()).isEqualByComparingTo(Status.STOPPED);
    }

    @Test
    @Order(4)
    void seek_after_media_player_is_ready(FxRobot robot) throws TimeoutException {
        waitForStatus(Status.READY);

        // Seek after media player is ready
        mediaPlayer.seek(Duration.seconds(120));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(mediaPlayer.getCurrentTime()).isEqualTo(Duration.seconds(120));
    }

    @Test
    @Order(5)
    void seek_after_media_player_is_playing(FxRobot robot) throws TimeoutException {
        waitForStatus(Status.READY);

        // Seek after media player is playing
        robot.clickOn(playButton);
        waitForStatus(Status.PLAYING);
        // robot click on seek slider
        final Duration seekTime = Duration.seconds(360);
        seekViaRobot(robot, seekTime);
        assertThat(mediaPlayer.getCurrentTime()).isGreaterThanOrEqualTo(seekTime);
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(seekTime.add(Duration.seconds(4))),
                mediaPlayer.currentTimeProperty()));

        robot.clickOn(stopButton);
        waitForStatus(Status.STOPPED);
    }

    @Test
    @Order(6)
    void seek_after_media_player_is_paused(FxRobot robot) throws TimeoutException {
        waitForStatus(Status.READY);

        // Seek after pause and then play again
        robot.clickOn(playButton);
        robot.clickOn(pauseButton);
        final Duration seekTime = Duration.seconds(483);
        mediaPlayer.seek(seekTime);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(mediaPlayer.getCurrentTime()).isEqualTo(Duration.seconds(483));
        robot.clickOn(playButton);
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(seekTime.add(Duration.seconds(5.9))),
                mediaPlayer.currentTimeProperty()));

        robot.clickOn(stopButton);
        waitForStatus(Status.STOPPED);
    }

    @Test
    @Order(7)
    void seek_negative_time(FxRobot robot) throws TimeoutException {
        waitForStatus(Status.READY);

        // Negative seek time
        mediaPlayer.seek(Duration.seconds(-1));
        assertThat(mediaPlayer.getError()).isNotNull();
        assertThat(mediaPlayer.getCurrentTime()).isEqualByComparingTo(Duration.ZERO);
        // Play
        robot.clickOn(playButton);
        // Play for 3 seconds
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, Bindings.createBooleanBinding(() ->
                        mediaPlayer.getCurrentTime().greaterThan(Duration.seconds(3)),
                mediaPlayer.currentTimeProperty()));

        robot.clickOn(stopButton);
        assertThat(mediaPlayer.getStatus()).isEqualByComparingTo(Status.STOPPED);
    }

    private void waitForStatus(Status status) throws TimeoutException {
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, mediaPlayer.statusProperty().isEqualTo(status));
        assertThat(mediaPlayer.getStatus()).isEqualByComparingTo(status);
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
