/**
 * Provides the set of classes for integrating audio and video playback and recording into JavaFX applications
 * that run on desktop/mobile devices and also on the web via JPro server using exactly the same code.
 * <p>This can be archived by using only the following classes: {@link one.jpro.platform.media.MediaSource},
 * {@link one.jpro.platform.media.player.MediaPlayer}, {@link one.jpro.platform.media.recorder.MediaRecorder} and
 * {@link one.jpro.platform.media.MediaView}.</p>
 *
 * <p>
 * <h4>Supported media player MIME types</h4>
 * <table style="width: 33%; border: 1px solid;">
 * <tr><th scope="col">Name</th><th scope="col" style="text-align: left">Value</th></tr>
 * <tr>
 *     <th scope="row">AIFF</th>
 *     <td>audio/x-aiff</td>
 * </tr>
 * <tr>
 *     <th scope="row">MP3</th>
 *     <td>audio/mp3</td>
 * </tr>
 * <tr>
 *     <th scope="row">MPA</th>
 *     <td>audio/mpeg</td>
 * </tr>
 * <tr>
 *     <th scope="row">WAV</th>
 *     <td>audio/x-wav</td>
 * </tr>
 * <tr>
 *     <th scope="row">MP4</th>
 *     <td>video/mp4</td>
 * </tr>
 * <tr>
 *     <th scope="row">M4A</th>
 *     <td>audio/x-m4a</td>
 * </tr>
 * <tr>
 *     <th scope="row">M4V</th>
 *     <td>video/x-m4v</td>
 * </tr>
 * <tr>
 *     <th scope="row">M3U8</th>
 *     <td>application/vnd.apple.mpegurl</td>
 * </tr>
 * <tr>
 *     <th scope="row">M3U</th>
 *     <td>audio/mpegurl</td>
 * </tr>
 * <tr>
 *     <th scope="row">MP2T</th>
 *     <td>video/MP2T</td>
 * </tr>
 * <tr>
 *     <th scope="row">FMP4</th>
 *     <td>video/quicktime</td>
 * </tr>
 * <tr>
 *     <th scope="row">AAC</th>
 *     <td>audio/aac</td>
 * </tr>
 * </table>
 * </p>
 *
 * <p>
 * <h4>Supported Protocols</h4>
 * <table style="width: 100%; border: 1px solid;">
 * <tr><th scope="col">Protocol</th><th scope="col" style="text-align: left">Description</th><th scope="col" style="text-align: left">Reference</th></tr>
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
 * </p>
 *
 * <p>
 * <h4>Error Handling</h4>
 * Errors using JPro Media are thrown immediately (synchronous), for example
 * when a <code>MediaPlayer</code> is created by calling
 * {@link one.jpro.platform.media.player.MediaPlayer#create(javafx.stage.Stage, MediaSource)}
 * or when <code>MediaRecorder</code> is created by calling
 * {@link one.jpro.platform.media.recorder.MediaRecorder#create(javafx.stage.Stage)}.
 * <p>Also errors are thrown later asynchronously, for example during the playback and recording process.
 * These errors are reported by the {@link one.jpro.platform.media.player.MediaPlayer#onErrorProperty()}
 * property by registering an error handler via
 * {@link one.jpro.platform.media.player.MediaPlayer#setOnError(javafx.event.EventHandler)} or
 * {@link one.jpro.platform.media.recorder.MediaRecorder#setOnError(javafx.event.EventHandler)}.
 * After an error has accured, use {@link one.jpro.platform.media.player.MediaPlayer#getError()} or
 * {@link one.jpro.platform.media.recorder.MediaRecorder#getError()} to get the error message.</p>
 * </p>
 *
 * @author Besmir Beqiri
 */
package one.jpro.platform.media;

