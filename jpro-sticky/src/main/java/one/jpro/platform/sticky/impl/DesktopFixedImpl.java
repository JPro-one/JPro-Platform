package one.jpro.platform.sticky.impl;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import one.jpro.platform.sticky.ScrollAnchor;
import one.jpro.platform.sticky.ScrollPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The desktop {@link ScrollPosition#FIXED} implementation: mounts the node into the per-scene
 * {@link StickyOverlay} and anchors it to the scene, re-resolving on scene resize. Pure JavaFX, no
 * {@link com.jpro.webapi.WebAPI}, no core change. On a plain desktop window (nothing scrolls) fixed
 * is exactly a scene-anchored overlay. In the browser, fixed goes through {@link WebScrollImpl}
 * instead.
 * <p>
 * The geometry is resolved by {@link AnchorGeometry} against the scene size, the same resolver the
 * web path uses against the browser viewport, so a fixed node lands in the identical place whether
 * the app runs on desktop or on the web.
 *
 * @author Tobias Horak
 */
public final class DesktopFixedImpl implements ScrollImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(DesktopFixedImpl.class);

    private final Node node;
    private final ScrollAnchor anchor;
    private final long stackOrder = StickyOverlay.nextStackOrder(ScrollPosition.FIXED);

    private Scene scene;
    private Group overlay;
    private Region placeholder;
    private Pane originalParent;
    private int originalIndex = -1;
    /** The node's scene x before mounting, used when the horizontal axis is left NATURAL. */
    private double originalX;

    private final InvalidationListener relayout = obs -> sync();
    private ChangeListener<Scene> sceneWaiter;
    /** Fires teardown when the placeholder (and thus the route subtree) leaves the scene. */
    private ChangeListener<Scene> placeholderSceneWaiter;
    private boolean torndown;

    public DesktopFixedImpl(Node node, ScrollAnchor anchor) {
        this.node = node;
        this.anchor = anchor;
    }

    @Override
    public void install() {
        if (node.getScene() != null) {
            attach();
        } else {
            sceneWaiter = (obs, old, s) -> {
                if (s != null) {
                    node.sceneProperty().removeListener(sceneWaiter);
                    sceneWaiter = null;
                    attach();
                }
            };
            node.sceneProperty().addListener(sceneWaiter);
        }
    }

    private void attach() {
        if (torndown) {
            return;
        }
        this.scene = node.getScene();
        final Parent parent = node.getParent();
        if (!(parent instanceof Pane)) {
            LOGGER.warn("jpro-sticky: node's parent is {} (not a Pane); cannot fix {}. Node stays in flow.",
                    parent == null ? "null" : parent.getClass().getSimpleName(), node);
            return;
        }
        final Group ov = StickyOverlay.overlayForNode(node);
        if (ov == null) {
            LOGGER.warn("jpro-sticky: no overlay host for {} (outside a scene, or host not a Pane/Group)."
                    + " Node stays in flow.", node);
            return;
        }
        this.overlay = ov;
        this.originalParent = (Pane) parent;
        this.originalIndex = originalParent.getChildren().indexOf(node);
        if (originalIndex < 0) {
            return;
        }
        this.originalX = node.localToScene(0, 0).getX();

        // Fixed is out of flow, so the placeholder is zero-height. Mirror the node's constraints and
        // width onto it so the surrounding layout doesn't shift, then mount the node into the shared
        // overlay (sorted by stackOrder).
        placeholder = new Region();
        placeholder.setMaxWidth(Double.MAX_VALUE);
        Placeholders.mirror(node, placeholder);
        placeholder.setPrefHeight(0);
        originalParent.getChildren().set(originalIndex, placeholder);
        node.setManaged(false);
        StickyOverlay.insertSorted(overlay, node, stackOrder);

        // End/center/stretch anchors depend on the scene size, and the node's own size can change too.
        scene.widthProperty().addListener(relayout);
        scene.heightProperty().addListener(relayout);
        node.layoutBoundsProperty().addListener(relayout);

        // The placeholder rides the flow, so it leaves the scene on route unmount. The reparented node
        // lives in the persistent overlay and never does. Tear down when the placeholder's scene goes
        // null, re-checking next pulse to skip a same-pulse detach/reattach.
        placeholderSceneWaiter = (obs, old, s) -> {
            if (s == null && !torndown) {
                Platform.runLater(() -> {
                    if (!torndown && placeholder != null && placeholder.getScene() == null) {
                        uninstall();
                    }
                });
            }
        };
        placeholder.sceneProperty().addListener(placeholderSceneWaiter);

        sync();
    }

    private void sync() {
        if (torndown || scene == null || overlay == null) {
            return;
        }
        final double availW = scene.getWidth();
        final double availH = scene.getHeight();
        if (AnchorGeometry.needsAvailableSize(anchor) && (availW <= 0 || availH <= 0)) {
            return;
        }
        final double naturalW = AnchorGeometry.naturalWidth(node);
        final AnchorGeometry g = AnchorGeometry.resolve(anchor, availW, availH,
                naturalW, w -> AnchorGeometry.naturalHeight(node, w),
                originalX, naturalW, true);
        node.resize(g.nodeW, g.nodeH);
        final Point2D local = overlay.sceneToLocal(g.x, g.y0);
        node.setLayoutX(local.getX());
        node.setLayoutY(local.getY());
    }

    @Override
    public void uninstall() {
        torndown = true;
        if (sceneWaiter != null) {
            node.sceneProperty().removeListener(sceneWaiter);
            sceneWaiter = null;
        }
        if (scene != null) {
            scene.widthProperty().removeListener(relayout);
            scene.heightProperty().removeListener(relayout);
        }
        node.layoutBoundsProperty().removeListener(relayout);
        if (placeholder != null && placeholderSceneWaiter != null) {
            placeholder.sceneProperty().removeListener(placeholderSceneWaiter);
            placeholderSceneWaiter = null;
        }

        StickyOverlay.remove(overlay, node);
        if (originalParent != null && placeholder != null) {
            final int idx = originalParent.getChildren().indexOf(placeholder);
            if (idx >= 0) {
                originalParent.getChildren().set(idx, node);
            }
            node.setManaged(true);
        }
    }
}
