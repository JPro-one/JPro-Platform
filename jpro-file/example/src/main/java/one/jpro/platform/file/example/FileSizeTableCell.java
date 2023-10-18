package one.jpro.platform.file.example;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.text.TextAlignment;
import one.jpro.platform.file.FileSource;
import org.apache.commons.io.FileUtils;

/**
 * A custom TableCell for displaying file sizes.
 *
 * @param <S> The type of the TableView source
 * @author Besmir Beqiri
 */
public class FileSizeTableCell<S extends FileSource> extends TableCell<S, Long> {

    public FileSizeTableCell() {
        setTextAlignment(TextAlignment.RIGHT);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        setAlignment(Pos.CENTER_RIGHT);
    }

    @Override
    protected void updateItem(Long size, boolean empty) {
        super.updateItem(size, empty);

        if (empty) {
            setText(null);
        } else {
            setText(FileUtils.byteCountToDisplaySize(size));
        }
    }
}
