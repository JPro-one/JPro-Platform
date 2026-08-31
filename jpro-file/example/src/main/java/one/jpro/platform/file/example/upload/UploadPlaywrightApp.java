package one.jpro.platform.file.example.upload;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.FileUploadProgress;
import one.jpro.platform.file.dropper.FileDropper;
import one.jpro.platform.file.picker.FileOpenPicker;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test app for the Playwright tests: a picker accepting any file, a picker and dropper
 * restricted to {@code .PNG}, and labels exposing the selection, upload status and the
 * combined {@link FileUploadProgress}.
 */
public class UploadPlaywrightApp extends Application {

    private final FileUploadProgress uploads = new FileUploadProgress();
    private final Label selected = new Label("");
    private final Label status = new Label("");
    private final Label future = new Label("");

    @Override
    public void start(Stage stage) {
        Label anyZone = zone("anyZone", "Pick any file");
        FileOpenPicker anyPicker = FileOpenPicker.create(anyZone);
        anyPicker.getExtensionFilters().add(ExtensionFilter.ANY);
        anyPicker.setOnFilesSelected(this::select);

        Label pngZone = zone("pngZone", "Pick or drop png");
        ExtensionFilter png = ExtensionFilter.of("Images", ".PNG");
        FileOpenPicker pngPicker = FileOpenPicker.create(pngZone);
        pngPicker.getExtensionFilters().add(png);
        pngPicker.setOnFilesSelected(this::select);
        FileDropper pngDropper = FileDropper.create(pngZone);
        pngDropper.setExtensionFilter(png);
        pngDropper.setOnFilesSelected(this::select);

        selected.setId("selected");
        status.setId("status");
        future.setId("future");
        Label progress = new Label("0.0");
        progress.setId("progress");
        uploads.progressProperty().addListener((o, ov, nv) -> progress.setText(String.valueOf(nv)));
        Label sizes = new Label("0/0");
        sizes.setId("sizes");
        Runnable updateSizes = () -> sizes.setText(uploads.getUploadedSize() + "/" + uploads.getTotalSize());
        uploads.uploadedSizeProperty().addListener((o, ov, nv) -> updateSizes.run());
        uploads.totalSizeProperty().addListener((o, ov, nv) -> updateSizes.run());

        Button upload = button("upload", "Upload", () -> {
            observeFutures();
            uploads.uploadAll();
        });
        Button uploadCancel = button("uploadCancel", "Upload + cancel", () -> {
            observeFutures();
            uploads.uploadAll();
            uploads.cancelAll();
        });

        VBox root = new VBox(10, anyZone, pngZone, upload, uploadCancel, selected, status, progress, sizes, future);
        root.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(root, 500, 500));
        stage.show();
    }

    private void select(List<? extends FileSource> files) {
        uploads.getFiles().setAll(files);
        selected.setText(files.stream().map(FileSource::getName).collect(Collectors.joining(",")));
        status.setText("");
        future.setText("");
        files.forEach(f -> f.uploadStatusProperty().addListener((o, ov, nv) -> status.setText(String.valueOf(nv))));
    }

    private void observeFutures() {
        future.setText("pending");
        for (FileSource f : uploads.getFiles()) {
            var completable = f.uploadFileAsync();
            new Thread(() -> {
                String result;
                try {
                    File file = completable.join();
                    result = file.exists() ? "SUCCESS" : "FAILED: missing file";
                } catch (Exception e) {
                    result = e.getClass().getSimpleName();
                }
                String r = result;
                Platform.runLater(() -> future.setText(r));
            }).start();
        }
    }

    private static Label zone(String id, String text) {
        Label label = new Label(text);
        label.setId(id);
        label.setMinSize(300, 60);
        label.setAlignment(Pos.CENTER);
        label.setStyle("-fx-border-color: gray;");
        return label;
    }

    private static Button button(String id, String text, Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.setOnAction(e -> action.run());
        return button;
    }
}
