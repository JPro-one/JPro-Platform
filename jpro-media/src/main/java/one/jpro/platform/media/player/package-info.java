/**
 * <h3>Media playback via {@link one.jpro.platform.media.player.MediaPlayer}</h3>
 * <p>
 * The basic steps required to play media are:
 * </p>
 * <ol>
 *     <li>Create a {@link one.jpro.platform.media.MediaSource} object for the desired media source.</li>
 *     <li>Create a {@link one.jpro.platform.media.player.MediaPlayer} object from the <code>MediaSource</code> object
 *     via {@link one.jpro.platform.media.player.MediaPlayer#create(javafx.stage.Stage, MediaSource)}.</li>
 *     <li>Create a {@link one.jpro.platform.media.MediaView} object via
 *     {@link one.jpro.platform.media.MediaView#create(MediaPlayer)}.</li>
 *     <li>Add the <code>MediaView</code> to the scene graph.</li>
 *     <li>Invoke {@link one.jpro.platform.media.player.MediaPlayer#play()}.</li>
 * </ol>
 * These steps are illustrated by the sample code in the {@link one.jpro.platform.media.MediaView} class documentation.
 * Some things which should be noted are:
 * <ul>
 *     <li>One <code>MediaSource</code> object may be shared among multiple <code>MediaPlayer</code>s.
 *     <li>Currently, one <code>MediaPlayer</code> may be shared among multiple <code>MediaView</code>s
 *         only in desktop/mobile application. In web application, each <code>MediaView</code> must have its own
 *         <code>MediaPlayer</code> object.</li>
 *     <li>Media may be played directly by a <code>MediaPlayer</code>
 *         without creating a <code>MediaView</code> although a view is required for display.</li>
 *     <li>Instead of {@link one.jpro.platform.media.player.MediaPlayer#play()},
 *         {@link one.jpro.platform.media.player.MediaPlayer#setAutoPlay MediaPlayer.setAutoPlay(true)}
 *         may be used to request that playing start as soon as possible. In web applications,
 *         the <code>MediaPlayer</code> may not start playing automatically when the web page
 *         is shown for the first time due to autoplay blocking policy of the browsers.</li>
 *         </li>
 *     <li><code>MediaPlayer</code> has several operational states defined by
 *         {@link javafx.scene.media.MediaPlayer.Status}.
 * </ul>
 *
 * @author Besmir Beqiri
 */
package one.jpro.platform.media.player;

import one.jpro.platform.media.MediaSource;
