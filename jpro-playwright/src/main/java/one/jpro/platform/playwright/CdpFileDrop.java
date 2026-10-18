package one.jpro.platform.playwright;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.BoundingBox;

import java.nio.file.Path;

/**
 * Drops files from the OS onto the page with native drag events via CDP {@code Input.dispatchDragEvent}, so the
 * browser targets and delivers the drop like a real one (Chromium only).
 */
public final class CdpFileDrop {

    private CdpFileDrop() {}

    /** Dispatches one drag event ({@code dragEnter}, {@code dragOver}, {@code drop} or {@code cancel}) at a point. */
    public static void dispatch(CDPSession cdp, String type, double x, double y, Path file) {
        JsonArray files = new JsonArray();
        files.add(file.toAbsolutePath().toString());
        JsonObject data = new JsonObject();
        data.add("items", new JsonArray());
        data.add("files", files);
        data.addProperty("dragOperationsMask", 1); // copy
        JsonObject params = new JsonObject();
        params.addProperty("type", type);
        params.addProperty("x", x);
        params.addProperty("y", y);
        params.add("data", data);
        cdp.send("Input.dispatchDragEvent", params);
    }

    /** Full enter → over → drop sequence at one point. */
    public static void drop(CDPSession cdp, double x, double y, Path file) {
        dispatch(cdp, "dragEnter", x, y, file);
        dispatch(cdp, "dragOver", x, y, file);
        dispatch(cdp, "drop", x, y, file);
    }

    /** Drops {@code file} onto the centre of {@code target}. */
    public static void drop(Locator target, Path file) {
        BoundingBox box = target.boundingBox();
        CDPSession cdp = target.page().context().newCDPSession(target.page());
        try {
            drop(cdp, box.x + box.width / 2, box.y + box.height / 2, file);
        } finally {
            cdp.detach();
        }
    }
}
