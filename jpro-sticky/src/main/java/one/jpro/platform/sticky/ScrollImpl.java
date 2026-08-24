package one.jpro.platform.sticky;

/**
 * A single node's installed scroll positioning, reversible via {@link #uninstall()}.
 * <p>
 * {@link Scroll} selects one implementation per node at install time and stashes it on the node so
 * a later {@code setScrollPosition} (or {@code clearScrollPosition}) can tear it down cleanly:
 * <ul>
 *   <li>{@link ScrollOverride}, the web path: a compositor scroll-timeline override, used for
 *       natively scrolled browser documents;</li>
 *   <li>{@link FXStickyImpl}, the desktop (and FX-scrolled {@code ScrollPane}) sticky path: pure
 *       JavaFX, pinning by {@code translate} within the scrolled content;</li>
 *   <li>{@link FXFixedImpl}, the desktop fixed path: a scene-root overlay anchored to the scene.</li>
 * </ul>
 * The split is invisible to callers: whichever implementation is chosen, the positioning behaviour
 * is the same.
 *
 * @author Tobias Horak
 */
interface ScrollImpl {

    /**
     * Installs the positioning. Depending on the implementation this may complete asynchronously
     * (once the node enters a scene, or once the {@link com.jpro.webapi.WebAPI} resolves).
     */
    void install();

    /**
     * Reverses everything {@link #install()} put in place: deregisters listeners, restores the
     * node to its normal flow parenting, and releases any host/overlay it created. A no-op if the
     * implementation never fully installed.
     */
    void uninstall();
}
