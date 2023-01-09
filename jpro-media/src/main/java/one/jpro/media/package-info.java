/**
 * Provides the set of classes for integrating audio and video playback and recording into JavaFX applications
 * that run on desktop/mobile devices and also on the web via JPro server using exactly the same code.
 * <p>This can be archived by using only the following classes: {@link one.jpro.media.MediaSource},
 * {@link one.jpro.media.player.MediaPlayer}, {@link one.jpro.media.recorder.MediaRecorder} and
 * {@link one.jpro.media.MediaView}.</p>
 *
 * <p>
 * <h4>Supported Protocols</h4>
 * <table border="1">
 * <caption>Supported Protocols Table</caption>
 * <tr><th scope="col">Protocol</th><th scope="col">Description</th><th scope="col">Reference</th></tr>
 * <tr>
 *     <th scope="row">FILE</th>
 *     <td>Protocol for URI representation of local files</td>
 *     <td><a href="https://docs.oracle.com/javase/17/docs/api/java/net/URI.html">java.net.URI</a></td>
 * </tr>
 * <tr>
 *     <th scope="row">HTTP</th>
 *     <td>Hypertext transfer protocol for representation of remote files</td>
 *     <td><a href="https://docs.oracle.com/javase/17/docs/api/java/net/URI.html">java.net.URI</a></td>
 * </tr>
 * <tr>
 *     <th scope="row">HTTPS</th>
 *     <td>Hypertext transfer protocol secure for representation of remote files</td>
 *     <td><a href="https://docs.oracle.com/javase/17/docs/api/java/net/URI.html">java.net.URI</a></td>
 * </tr>
 * <tr>
 *     <th scope="row">JAR</th>
 *     <td>Representation of media entries in files accessible via the FILE, HTTP or HTTPS protocols</td>
 *     <td><a href="https://docs.oracle.com/javase/17/docs/api/java/net/JarURLConnection.html">java.net.JarURLConnection</a></td>
 * </tr>
 * </table>
 *
 * <h4>Error Handling</h4>
 * <p>
 * Errors using JPro Media are thrown immediately (synchronous), for example
 * when a <code>MediaPlayer</code> is created by calling
 * {@link one.jpro.media.player.MediaPlayer#create(javafx.stage.Stage, one.jpro.media.MediaSource)}
 * or when <code>MediaRecorder</code> is created by calling
 * {@link one.jpro.media.recorder.MediaRecorder#create(javafx.stage.Stage)}.
 * <p>Also errors are thrown later asynchronously, for example during the playback and recording process.
 * These errors are reported by the {@link one.jpro.media.player.MediaPlayer#onErrorProperty()}
 * property by registering an error handler via
 * {@link one.jpro.media.player.MediaPlayer#setOnError(javafx.event.EventHandler)} or
 * {@link one.jpro.media.recorder.MediaRecorder#setOnError(javafx.event.EventHandler)}.
 * After an error has accured, use {@link one.jpro.media.player.MediaPlayer#getError()} or
 * {@link one.jpro.media.recorder.MediaRecorder#getError()} to get the error message.</p>
 *
 * @author Besmir Beqiri
 */
package one.jpro.media;