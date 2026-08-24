package one.jpro.platform.sticky;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The overlay that hosts reparented sticky/fixed nodes, shared by the web ({@link ScrollOverride})
 * and desktop ({@link FXFixedImpl}) implementations so the stacking order is one consistent model
 * regardless of which mechanism mounts a node.
 * <p>
 * <strong>Where the overlay lands.</strong> At pin time the node is resolved to its
 * {@linkplain #hostFor host}: the nearest ancestor {@link Pane} stamped by
 * {@link #registerHost} ({@code Scroll.registerOverlayHost}), or the scene root when none is
 * registered. Each host carries its own overlay {@link Group}, created lazily and removed again when
 * its last pin detaches. Mounting below a registered host (one that sits under the app's CSS scope
 * and its parent-chain context holders) keeps the reparented node a real {@code getParent()}
 * descendant of them, so route CSS, colour tokens and every parent-chain context resolve exactly as
 * they do in flow; with no host registered the node lands at the scene root.
 * <p>
 * Every mounted node is assigned a {@linkplain #nextStackOrder(ScrollPosition) stack order}: a type
 * tier ({@link ScrollPosition#FIXED} above {@link ScrollPosition#STICKY}) above the order
 * {@code setScrollPosition} is called, and {@link #insertSorted} keeps the overlay's children ordered
 * by it. So within a host overlay the default front-to-back order is fixed-over-sticky, add-order
 * within each tier (a later-declared node paints on top, CSS's source-order tiebreaker), rather than
 * the async order in which installs happen to complete. {@code viewOrder} remains the explicit
 * per-node override (JPro/JavaFX sort by it first, and the web path never sets it), so a consumer can
 * still force any order. The add-order guarantee is per host: pins in different hosts stack by their
 * hosts' scene-graph positions, not by global add-order (a non-issue when pins share one host).
 *
 * @author Tobias Horak
 */
final class StickyOverlay {

    /** {@link Parent} (host) property key under which that host's overlay {@link Group} is cached. */
    private static final Object OVERLAY_KEY = new Object();

    /** {@link Pane} property key stamped by {@link #registerHost} marking a registered overlay host. */
    private static final Object HOST_KEY = new Object();

    /** {@link Node} property key stashing a mounted node's stack order, read by sibling mounts. */
    private static final Object STACK_ORDER_KEY = new Object();

    /** {@code id} stamped on every overlay {@link Group}; lets {@link #isOverlay} spot a mounted node. */
    private static final String OVERLAY_ID = "jpro-sticky-overlay";

    /**
     * {@code viewOrder} for the overlay {@link Group} itself, so it paints above the host's other
     * children (the routed page content) regardless of child-list order. This matters when the host is
     * a routing container that swaps its content: the new page is appended <em>after</em> the overlay,
     * and in a {@link Pane} later children paint on top, so without this the pinned node would be buried
     * behind the new page after every navigation. Negative = in front (JPro/JavaFX sort by viewOrder
     * first). This is the overlay's own order among host siblings; the per-node viewOrder override
     * inside the overlay is untouched.
     */
    private static final double OVERLAY_VIEW_ORDER = -1.0;

    /**
     * Monotonic order in which mounts are created (i.e. the order {@code setScrollPosition} is
     * called), so paint/stacking order follows source order rather than install-completion order.
     */
    private static final AtomicLong STACK_SEQ = new AtomicLong();

    /** Bit width of the add-sequence below the type tier in a {@linkplain #nextStackOrder stack key}. */
    private static final int TIER_SHIFT = 48;

    private StickyOverlay() {
        // utility class
    }

    /**
     * Returns the next stack-order key for a mount, folding a type tier above the add-sequence so that,
     * within one host overlay, {@link ScrollPosition#FIXED} nodes sort after (paint in front of)
     * {@link ScrollPosition#STICKY} nodes regardless of the order they were applied, and within a tier
     * the later-applied node paints on top (source order). {@code viewOrder} remains the explicit
     * per-node override (JPro/JavaFX sort children by it first, and the web path never sets it), so a
     * consumer can still force any order. Assign once per mounted node, at construction.
     *
     * @param position the mount's positioning mode; {@code FIXED} tiers above everything else
     * @return the stack key ({@code tier} in the high bits, add-sequence in the low bits)
     */
    static long nextStackOrder(ScrollPosition position) {
        final long tier = (position == ScrollPosition.FIXED) ? 1L : 0L;
        final long seq = STACK_SEQ.getAndIncrement() & ((1L << TIER_SHIFT) - 1);
        return (tier << TIER_SHIFT) | seq;
    }

    /**
     * Stamps {@code host} as an overlay host, so pins under it mount into a host-local overlay rather
     * than the scene root. Idempotent. See {@code Scroll.registerOverlayHost} for the public contract.
     *
     * @param host the pane to register; must not be {@code null}
     */
    static void registerHost(Pane host) {
        host.getProperties().put(HOST_KEY, Boolean.TRUE);
    }

    /**
     * Resolves and returns the overlay a node should mount into: the {@link Group} of its
     * {@linkplain #hostFor nearest registered host} (else the scene root), created on first use.
     * Must be called while the node is still in its flow position (its real {@code getParent()}
     * chain is walked to find the host).
     *
     * @param node the node about to be pinned; must not be {@code null}
     * @return the overlay to mount into, or {@code null} if the node is outside a scene or the
     *         resolved host cannot carry an overlay
     */
    static Group overlayForNode(Node node) {
        final Scene scene = node.getScene();
        if (scene == null) {
            return null;
        }
        final Parent host = hostFor(node, scene);
        return (host == null) ? null : overlayIn(host);
    }

    /**
     * Walks up {@code node}'s real parent chain to the nearest {@link Pane} stamped by
     * {@link #registerHost}; falls back to the scene root when none is registered.
     */
    private static Parent hostFor(Node node, Scene scene) {
        for (Node cur = node.getParent(); cur != null; cur = cur.getParent()) {
            if (cur instanceof Pane && cur.getProperties().get(HOST_KEY) == Boolean.TRUE) {
                return (Parent) cur;
            }
        }
        return scene.getRoot();
    }

    /**
     * Returns {@code host}'s overlay, creating it on first use. A {@link Group} (not a {@link Pane}):
     * JPro picks server-side in the FX graph, so a Group's pick is the union of its children (empty =
     * transparent to clicks) whereas a full-document Pane would swallow them. Unmanaged and left at
     * layout origin, so it shares the host's coordinate space.
     *
     * @param host the resolved host (a registered pane or the scene root); must not be {@code null}
     * @return the host's overlay, or {@code null} if the host cannot carry one
     */
    private static Group overlayIn(Parent host) {
        final Object existing = host.getProperties().get(OVERLAY_KEY);
        if (existing instanceof Group) {
            return (Group) existing;
        }
        final Group ov = new Group();
        ov.setManaged(false);
        ov.setId(OVERLAY_ID);
        ov.setViewOrder(OVERLAY_VIEW_ORDER);
        if (host instanceof Pane) {
            ((Pane) host).getChildren().add(ov);
        } else if (host instanceof Group) {
            ((Group) host).getChildren().add(ov);
        } else {
            return null;
        }
        host.getProperties().put(OVERLAY_KEY, ov);
        return ov;
    }

    /**
     * Adds {@code node} to {@code overlay} at the index that keeps the overlay's children ordered by
     * {@code stackOrder} ascending, so a later-declared node ends up later in the list (painted on
     * top). Stashes the order on the node so sibling mounts can read it.
     *
     * @param overlay    the overlay to mount into; must not be {@code null}
     * @param node       the node to mount; must not be {@code null}
     * @param stackOrder the node's {@linkplain #nextStackOrder() stack order}
     */
    static void insertSorted(Group overlay, Node node, long stackOrder) {
        node.getProperties().put(STACK_ORDER_KEY, stackOrder);
        final var kids = overlay.getChildren();
        int insertAt = kids.size();
        for (int i = 0; i < kids.size(); i++) {
            if (stackOrderOf(kids.get(i)) > stackOrder) {
                insertAt = i;
                break;
            }
        }
        kids.add(insertAt, node);
    }

    /**
     * Removes a mounted node from the overlay and drops its stashed stack order. When that leaves the
     * overlay empty it is detached from its host and the host's cached-overlay stamp is cleared, so a
     * host (e.g. a reused popup container) never accumulates stale, empty overlays across navigations.
     * A no-op if {@code overlay} is {@code null} (the node was never mounted).
     *
     * @param overlay the overlay the node was mounted into, or {@code null}
     * @param node    the node to remove; must not be {@code null}
     */
    static void remove(Group overlay, Node node) {
        if (overlay != null) {
            overlay.getChildren().remove(node);
            if (overlay.getChildren().isEmpty()) {
                final Parent host = overlay.getParent();
                if (host instanceof Pane) {
                    ((Pane) host).getChildren().remove(overlay);
                } else if (host instanceof Group) {
                    ((Group) host).getChildren().remove(overlay);
                }
                if (host != null) {
                    host.getProperties().remove(OVERLAY_KEY);
                }
            }
        }
        node.getProperties().remove(STACK_ORDER_KEY);
    }

    /**
     * Whether {@code parent} is one of jpro-sticky's overlay {@link Group}s. Used by the async web
     * attach to tell a still-mounted node (a superseded application racing this one) apart from a real
     * flow parent, so it never mistakes the overlay for the flow slot.
     *
     * @param parent the node's current parent, or {@code null}
     * @return {@code true} if {@code parent} is a sticky overlay
     */
    static boolean isOverlay(Node parent) {
        return parent instanceof Group && OVERLAY_ID.equals(parent.getId());
    }

    /** The stack order stashed on a mounted node, or {@link Long#MIN_VALUE} if absent. */
    private static long stackOrderOf(Node n) {
        final Object v = n.getProperties().get(STACK_ORDER_KEY);
        return (v instanceof Long) ? (Long) v : Long.MIN_VALUE;
    }
}
