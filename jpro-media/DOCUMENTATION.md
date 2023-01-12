# JPro Media
JPro Media is a Java Library for playing and recording audio and video files in JavaFX applications that run
on both desktop/mobile devices and in the browser via [JPro](https://www.jpro.one) with exactly the same code. 
Building cross-platform applications with JPro Media is as easy as using the 
[JavaFX Media](https://openjfx.io/javadoc/17/javafx.media/module-summary.html) API.

## Features
- Write Once Run Anywhere (the same code runs on desktop/mobile devices and in the web browser)
- Playback of audio and video files or streams
- Recording the audio and video stream from a camera device
- Support for all common audio and video formats
- Unified media source API for local and remote media (inside the client's browser)

## Supported Platforms
- Web (via [JPro](https://www.jpro.one))
- Desktop (Windows, Linux, MacOS)

## Getting Started
To get started with JPro Media, you need to add the following configuration to your project.
### Gradle
By using the JPro Gradle plugin, we just need to add the `jpro-media` dependency to the `build.gradle` file: 
```groovy
dependencies {
    implementation 'one.jpro.jproutils:jpro-media:0.2.3-SNAPSHOT'
}
```
### Maven
By using the JPro Maven plugin, we just need to add the `jpro-media` dependency to the `pom.xml` file.
```xml
<dependencies>
    <dependency>
        <groupId>one.jpro.jproutils</groupId>
        <artifactId>jpro-media</artifactId>
        <version>0.2.3-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Similarities and differences to JavaFX Media
 - The `MediaSource` class is very similar to the `Media` class from the JavaFX Media API.
 - A `MediaPlayer` instance can be created by calling `MediaPlayer#create(Stage, MediaSource)`,
    while in JavaFX Media you would use `new MediaPlayer(Media)`.
 - The `MediaView` class is very similar to the `MediaView` class from the JavaFX Media API.
   It can be created by calling `MediaView#create(Stage, MediaPlayer)`, while in JavaFX Media you 
   would use `new MediaView(MediaPlayer)`.
   - The `MediaRecorder` can be created by calling `MediaRecorder#create(Stage)`,
   while in JavaFX Media doesn't provide a recorder.

## Usage
### Media Player API
For the playback functionality, the JPro Media API is very similar to the JavaFX Media API.
The main difference is that the `Media` class is replaced by the `MediaSource` class.
The following example shows how to play a video file:
```java
class JProMediaApplication extends Application {
    
    public void start(Stage stage) {
        stage.setTitle("JPro Media Player");

        // Get the media source as an application argument.
        String source = getParameters().getRaw().get(0);
        MediaSource mediaSource = new MediaSource(source);

        // Media controls
        Button playPauseButton = new Button("Play");
        playPauseButton.setDisable(true);
        Button stopButton = new Button("Stop");
        stopButton.setDisable(true);

        // Control events
        playPauseButton.setOnAction(event -> {
            if (mediaPlayer.getStatus() == Status.READY ||
                    mediaPlayer.getStatus() == Status.PAUSED ||
                    mediaPlayer.getStatus() == Status.STOPPED) {
                mediaPlayer.play();
            } else {
                mediaPlayer.pause();
            }
        });
        stopButton.setOnAction(event -> mediaPlayer.stop());

        // Event handlers
        mediaPlayer.setOnReady(event -> {
            playPauseButton.setDisable(false);
            stopButton.setDisable(false);
        });
        mediaPlayer.setOnPlaying(event -> playPauseButton.setText("Pause"));
        mediaPlayer.setOnPause(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnStopped(event -> playPauseButton.setText("Play"));
        mediaPlayer.setOnError(event -> System.out.println("Error: " + mediaPlayer.getError()));

        // User interface
        FlowPane controlsPane = new FlowPane(playPauseButton, stopButton);
        controlsPane.setAlignment(Pos.CENTER);
        controlsPane.setHgap(8);
        controlsPane.setVgap(8);
        controlsPane.setPadding(new Insets(8));
        VBox rootPane = new VBox(mediaView, controlsPane);
        VBox.setVgrow(mediaView, Priority.ALWAYS);

        Scene scene = new Scene(rootPane, 1140, 640);
        stage.setScene(scene);
        stage.show();
    }
}
```

### Media Recorder API
The JPro Media also provides a MediaRecorder API for recording the audio and video stream from a camera device.
The following example shows how to record from the default camera device:
```java
class JProMediaApplication extends Application {
    
    public void start(Stage stage) {
        stage.setTitle("JPro Camera Recorder");

        Button enableCamButton = new Button("Enable Cam");
        Button startButton = new Button("Start");
        startButton.setDisable(true);
        Button stopButton = new Button("Stop");
        stopButton.setDisable(true);
        Button saveButton = new Button("Save As...");
        saveButton.setDisable(true);

        StackPane previewPane = new StackPane(enableCamButton);

        MediaRecorder mediaRecorder = MediaRecorder.create(stage);
        MediaView cameraView = MediaView.create(mediaRecorder);

        FlowPane controlsPane = new FlowPane(startButton, stopButton, saveButton);
        controlsPane.setAlignment(Pos.CENTER);
        controlsPane.setHgap(8);
        controlsPane.setVgap(8);
        controlsPane.setPadding(new Insets(8));

        // Button events
        enableCamButton.setOnAction(event -> mediaRecorder.enable());
        startButton.setOnAction(event -> mediaRecorder.start());
        stopButton.setOnAction(event -> mediaRecorder.stop());
        saveButton.setOnAction(event -> {
            try {
                MediaUtil.retrieve(stage, mediaRecorder.getMediaSource());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Media recorder events
        mediaRecorder.setOnReady(event -> {
            startButton.setDisable(false);
            previewPane.getChildren().setAll(cameraView);
        });
        mediaRecorder.setOnStart(event -> {
            startButton.setDisable(true);
            stopButton.setDisable(false);
            saveButton.setDisable(true);
        });
        mediaRecorder.setOnStopped(event -> {
            startButton.setDisable(false);
            stopButton.setDisable(true);
            saveButton.setDisable(false);
        });
        mediaRecorder.setOnError(event -> System.out.println(mediaRecorder.getError().toString()));

        VBox rootPane = new VBox(previewPane, controlsPane);
        VBox.setVgrow(previewPane, Priority.ALWAYS);
        cameraView.fitWidthProperty().bind(rootPane.widthProperty());
        cameraView.fitHeightProperty().bind(rootPane.heightProperty()
                .subtract(controlsPane.heightProperty()));

        Scene scene = new Scene(rootPane, 760, 540);
        stage.setScene(scene);
        stage.show();
    }
}
```