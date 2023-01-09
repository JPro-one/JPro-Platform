/**
 * <h3>Media playback via {@link one.jpro.media.player.MediaPlayer}</h3>
 * <p>
 * The basic steps required to play media are:
 * </p>
 * <ol>
 *     <li>Create a {@link one.jpro.media.MediaSource} object for the desired media source.</li>
 *     <li>Create a {@link one.jpro.media.player.MediaPlayer} object from the <code>MediaSource</code> object
 *     via {@link one.jpro.media.player.MediaPlayer#create(javafx.stage.Stage, one.jpro.media.MediaSource)}.</li>
 *     <li>Create a {@link one.jpro.media.MediaView} object via
 *     {@link one.jpro.media.MediaView#create(one.jpro.media.player.MediaPlayer)}.</li>
 *     <li>Add the <code>MediaView</code> to the scene graph.</li>
 *     <li>Invoke {@link one.jpro.media.player.MediaPlayer#play()}.</li>
 * </ol>
 * These steps are illustrated by the sample code in the {@link one.jpro.media.MediaView} class documentation.
 * Some things which should be noted are:
 * <ul>
 *     <li>One <code>MediaSource</code> object may be shared among multiple <code>MediaPlayer</code>s.
 *     <li>One <code>MediaPlayer</code> may be shared among multiple <code>MediaView</code>s.
 *     <li>Media may be played directly by a <code>MediaPlayer</code>
 *         without creating a <code>MediaView</code> although a view is required for display.</li>
 *     <li>Instead of {@link one.jpro.media.player.MediaPlayer#play()},
 *         {@link one.jpro.media.player.MediaPlayer#setAutoPlay MediaPlayer.setAutoPlay(true)}
 *         may be used to request that playing start as soon as possible.</li>
 *     <li><code>MediaPlayer</code> has several operational states defined by
 *         {@link javafx.scene.media.MediaPlayer.Status}.
 * </ul>
 *
 * @author Besmir Beqiri
 */
package one.jpro.media.player;