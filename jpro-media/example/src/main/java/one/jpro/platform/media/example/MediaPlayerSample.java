package one.jpro.platform.media.example;

import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.NativeFileSource;
import one.jpro.platform.file.WebFileSource;
import one.jpro.platform.file.picker.FileOpenPicker;
import one.jpro.platform.media.MediaSource;
import one.jpro.platform.media.MediaView;
import one.jpro.platform.media.player.MediaPlayer;

import java.net.URL;
import java.util.Optional;

/**
 * Media player example.
 *
 * @author Besmir Beqiri
 */
public class MediaPlayerSample extends Application {

    public static final String MEDIA_SOURCE =
            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

    private static final ExtensionFilter videoExtensionFilter =
            ExtensionFilter.of("Video files", ".mp4");

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Button playPauseButton;
    private Button stopButton;
    private Slider seekSlider;
    private CheckBox preserveRatioCheckBox;
    private CheckBox muteCheckBox;
    private Slider volumeSlider;


    @Override
    public void start(Stage stage) {
        stage.setTitle("JPro Media Player");
        Scene scene = new Scene(createRoot(stage), 1180, 640);
        Optional.ofNullable(Theme.class.getResource(new CupertinoLight().getUserAgentStylesheet()))
                .map(URL::toExternalForm)
                .ifPresent(scene::setUserAgentStylesheet);
        stage.setScene(scene);
        stage.show();
    }

    public Parent createRoot(Stage stage) {
        // Controls
        mediaView = MediaView.create(stage);

        Button openButton = new Button("Open");
        playPauseButton = new Button("Play");
        playPauseButton.setDisable(true);
        stopButton = new Button("Stop");
        stopButton.setDisable(true);
        seekSlider = new Slider();
        seekSlider.setPrefWidth(480);
        preserveRatioCheckBox = new CheckBox("Preserve Ratio");
        muteCheckBox = new CheckBox("Mute");
        volumeSlider = new Slider();

        // Media player
        createMediaPlayer(stage, new MediaSource(MEDIA_SOURCE));

        // Configure the file open picker
        FileOpenPicker fileOpenPicker = FileOpenPicker.create(openButton);
        fileOpenPicker.setSelectedExtensionFilter(videoExtensionFilter);
        fileOpenPicker.setOnFilesSelected(fileSources -> fileSources.stream().findFirst().ifPresent(fileSource -> {
            mediaPlayer.stop();
            if (fileSource instanceof NativeFileSource nativeFileSource) {
                createMediaPlayer(stage, new MediaSource(nativeFileSource.getPlatformFile()));
            } else if (fileSource instanceof WebFileSource webFileSource) {
                createMediaPlayer(stage, new MediaSource(webFileSource.getPlatformFile()));
            }
        }));

        // User interface
        FlowPane controlsPane = new FlowPane(openButton, playPauseButton, stopButton, seekSlider,
                preserveRatioCheckBox, muteCheckBox, volumeSlider);
        controlsPane.getStyleClass().add("controls-pane");
        VBox rootPane = new VBox(mediaView, controlsPane);
        rootPane.getStyleClass().add("root-pane");
        VBox.setVgrow(mediaView, Priority.ALWAYS);

        Optional.ofNullable(getClass().getResource("css/media_sample.css"))
                .map(URL::toExternalForm)
                .ifPresent(cssResource -> rootPane.getStylesheets().add(cssResource));

        // Stop media player and release resources when we switch views via routing links
        rootPane.sceneProperty().addListener(observable -> {
            if (rootPane.getScene() == null) {
                mediaPlayer.stop();
            }
        });

        return rootPane;
    }

    private void createMediaPlayer(Stage stage, MediaSource mediaSource) {
        // Media player
        mediaPlayer = MediaPlayer.create(stage, mediaSource);
        mediaView.setMediaEngine(mediaPlayer);

        preserveRatioCheckBox.setSelected(mediaView.isPreserveRatio());
        volumeSlider.setValue(mediaPlayer.getVolume() * 100.0);

        // Control events
        stopButton.setOnAction(event -> {
            mediaPlayer.stop();
            seekSlider.setValue(0);
        });

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

        muteCheckBox.setOnAction(event -> {
            mediaPlayer.setMute(muteCheckBox.isSelected());
            volumeSlider.setDisable(muteCheckBox.isSelected());
        });

        volumeSlider.valueProperty().addListener(observable ->
                mediaPlayer.setVolume(volumeSlider.getValue() / 100.0));
        mediaPlayer.volumeProperty().addListener(observable -> {
            if (!volumeSlider.isValueChanging()) {
                volumeSlider.setValue(mediaPlayer.getVolume() * 100.0);
            }
        });

        // Player event handlers
        mediaPlayer.setOnReady(event -> {
            playPauseButton.setOnAction(event2 -> mediaPlayer.play());
            playPauseButton.setDisable(false);
            stopButton.setDisable(false);
            seekSlider.setValue(mediaPlayer.getCurrentTime().toSeconds());
            seekSlider.setMax(mediaPlayer.getDuration().toSeconds());
            volumeSlider.setValue(mediaPlayer.getVolume() * 100.0);
        });
        mediaPlayer.setOnPlaying(event -> {
            playPauseButton.setText("Pause");
            playPauseButton.setOnAction(event2 -> mediaPlayer.pause());
        });
        mediaPlayer.setOnPaused(event -> {
            playPauseButton.setText("Play");
            playPauseButton.setOnAction(event2 -> mediaPlayer.play());
        });
        mediaPlayer.setOnStopped(event -> {
            playPauseButton.setText("Play");
            playPauseButton.setOnAction(event2 -> mediaPlayer.play());
        });
        mediaPlayer.setOnEndOfMedia(event -> {
            playPauseButton.setText("Play");
            playPauseButton.setOnAction(event2 -> mediaPlayer.play());
        });
        mediaPlayer.setOnError(event -> System.out.println(mediaPlayer.getError().toString()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}