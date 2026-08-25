package one.jpro.platform.sticky.impl;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import one.jpro.platform.sticky.ScrollAnchor;
import one.jpro.platform.sticky.ScrollPosition;

/**
 * The desktop {@link ScrollPosition#FIXED} implementation: mounts the node into the per-scene
 * {@link StickyOverlay} (via {@link OverlayMount}) and anchors it to the scene, re-resolving on scene
 * resize. Pure JavaFX, no {@link com.jpro.webapi.WebAPI}, no core change. On a plain desktop window
 * (nothing scrolls) fixed is exactly a scene-anchored overlay. In the browser, fixed goes through
 * {@link WebScrollImpl} instead.
 * <p>
 * The geometry is resolved by {@link AnchorGeometry} against the scene size, the same resolver the
 * web path uses against the browser viewport, so a fixed node lands in the identical place whether
 * the app runs on desktop or on the web.
 *
 * @author Tobias Horak
 */
public final class DesktopFixedImpl implements ScrollImpl {

    private final Node node;
    private final ScrollAnchor anchor;
    /** Called when the flow slot leaves the scene, so the dispatcher can re-pin on re-entry. */
    private final Runnable onDetach;
    /** The shared reparent-into-overlay mechanic (flow slot, placeholder, overlay). */
    private final OverlayMount mount;

    private Scene scene;
    private Group overlay;
    private Region placeholder;
    /** The node's scene x before mounting, used when the horizontal axis is left NATURAL. */
    private double originalX;

    private final InvalidationListener relayout = obs -> sync();
    /** Fires teardown when the placeholder (and thus the route subtree) leaves the scene. */
    private ChangeListener<Scene> placeholderSceneWaiter;
    private boolean torndown;

    public DesktopFixedImpl(Node node, ScrollAnchor anchor, Runnable onDetach) {
        this.node = node;
        this.anchor = anchor;
        this.onDetach = onDetach;
        this.mount = new OverlayMount(node, StickyOverlay.nextStackOrder(ScrollPosition.FIXED));
    }

    @Override
    public void install() {
        // dispatcher only installs once the node is in a scene, so attach() can resolve the host now.
        attach();
    }

    private void attach() {
        if (torndown) {
            return;
        }
        this.scene = node.getScene();
        // capture the flow x before mounting (node still in its slot), for a NATURAL horizontal axis.
        this.originalX = node.localToScene(0, 0).getX();

        final Region ph = mount.mount();
        if (ph == null) {
            return; // could not mount, node stays in flow
        }
        this.placeholder = ph;
        this.overlay = mount.overlay();
        // fixed is out of flow, so its slot collapses to zero height.
        placeholder.setPrefHeight(0);

        // end/center/stretch anchors depend on the scene size, and the node's own size can change too.
        scene.widthProperty().addListener(relayout);
        scene.heightProperty().addListener(relayout);
        node.layoutBoundsProperty().addListener(relayout);

        // placeholder rides the flow, so it leaves the scene on route unmount (the node never does).
        // re-check next pulse to ignore a transient same-pulse detach/reattach.
        placeholderSceneWaiter = (obs, old, s) -> {
            if (s == null && !torndown) {
                Platform.runLater(() -> {
                    if (!torndown && placeholder != null && placeholder.getScene() == null) {
                        // hand back to the dispatcher: uninstall this delegate but stay alive to re-pin
                        // if the route returns. fall back to a direct uninstall if unwired.
                        if (onDetach != null) {
                            onDetach.run();
                        } else {
                            uninstall();
                        }
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
        if (scene != null) {
            scene.widthProperty().removeListener(relayout);
            scene.heightProperty().removeListener(relayout);
        }
        node.layoutBoundsProperty().removeListener(relayout);
        if (placeholder != null && placeholderSceneWaiter != null) {
            placeholder.sceneProperty().removeListener(placeholderSceneWaiter);
            placeholderSceneWaiter = null;
        }
        mount.unmount();
    }
}
