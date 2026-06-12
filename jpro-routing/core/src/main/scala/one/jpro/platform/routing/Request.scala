package one.jpro.platform.routing

import javafx.scene.Node
import one.jpro.platform.routing.LinkUtil.isValidLink
import org.slf4j.{Logger, LoggerFactory}

import java.lang.ref.WeakReference
import java.net.URI
import java.util.{Map => JMap}

/**
 * An incoming request, carrying the parsed URL (protocol, domain, port, path,
 * query parameters) that a Route maps to a Response. When a route is mounted via
 * {@code Route.path}, the request's path is relative to the mount point — the
 * original path stays available via {@code getOriginalPath()}.
 */
case class Request (
  private val url: String,
  private val protocol: String,
  private val domain: String,
  private val port: Int,
  private val origPath: String,
  private val path: String,
  private val directory: String,
  private val queryParameters: Map[String,String],
  private val origOldContent: WeakReference[Node],
  private val oldContent: WeakReference[Node]
) {

  assert(resolve(path) == origPath, s"resolve path: ${resolve(path)} != origPath: ${origPath}")

  /** Returns the full URL this request was created from. */
  def getUrl(): String = url
  /** Returns the protocol, e.g. "http" — may be null for plain paths. */
  def getProtocol(): String = protocol
  /** Returns the domain, e.g. "example.com" — may be null for plain paths. */
  def getDomain(): String = domain
  /** Returns the port, or -1 when not specified. */
  def getPort(): Int = port
  /** Returns the path as requested, unaffected by {@code Route.path} mounting. */
  def getOriginalPath(): String = origPath
  /** Returns the path relative to the current mount point (see {@code Route.path}). */
  def getPath(): String = path
  /** Returns the directory this request is mounted at, "/" by default. */
  def getDirectory(): String = directory
  /** Returns the query parameter, or an empty Option when absent. */
  def getQueryParameter(key: String): Option[String] = queryParameters.get(key)
  /** Returns the query parameter, or the default when absent. */
  def getQueryParameterOrElse(key: String, default: String): String = queryParameters.getOrElse(key, default)

  private lazy val immutableJavaMap: JMap[String, String] = {
    import scala.collection.JavaConverters._
    queryParameters.asJava
  }
  def getQueryParameters(): JMap[String,String] = immutableJavaMap
  def getOriginalOldContent(): WeakReference[Node] = origOldContent
  def getOldContent(): WeakReference[Node] = oldContent

  def getQueryParametersScala(): Map[String,String] = queryParameters

  def resolve(path: String): String = {
    assert(path.startsWith("/") || path.startsWith("./") || path.startsWith("../"), s"Path must start with / or ./ or ../ but was: ${path}")

    if(directory == "/") {
      path
    } else {
      if(path.startsWith(".")) {
        directory + "/" + path
      } else {
        directory + path
      }
    }
  }

  def mapContent(f: Node => Node) = {
    val oldContentV = oldContent.get()
    val oldContentVW = if(oldContentV eq null) {
      Request.wref_null
    } else {
      new WeakReference(f(oldContentV))
    }
    this.copy(oldContent = oldContentVW)
  }
}
object Request {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private var wref_null = new WeakReference[Node](null)

  def fromString(s: String, oldView: Node): Request = {
    val oldViewW = new WeakReference(oldView)
    Request.fromString(s).copy(oldContent = oldViewW, origOldContent = oldViewW)
  }
  def fromString(s: String): Request = {
    try {
      if(!isValidLink(s)) {
        logger.warn("Warning - Invalid Link: " + s)
      }
      val uri = new URI(s)
      val rawQuery = uri.getRawQuery
      val query: Map[String,String] = if(rawQuery == null || rawQuery == "") Map() else rawQuery.split("&").map(x => {
        val Array(a,b) = x.split("=")
        a -> b
      }).toMap
      val path = uri.getPath
      val res = Request(s, uri.getScheme, uri.getHost, uri.getPort, path,path,"/", query,wref_null,wref_null)
      res
    } catch {
      case e: Exception =>
        throw new RuntimeException("Could not parse Request from string: " + s, e)
    }
  }

  /**
   * Check whether it matches.
   * url can be like "/test" but also a full url like "http://domain:port/test"
   * The check ignores query parameters.
   */
  def matchesSoft(request: Request, url: String): Boolean = {
    try {
      val uri = new URI(url)
      val urlPath = uri.getPath

      // quick path check
      if (urlPath != request.getPath()) return false

      val hasHost = uri.getHost != null
      if (!hasHost) {
        // path-only comparison, queries ignored
        true
      } else {
        // full URL provided: compare scheme, host, and optionally port
        val schemeMatches = Option(uri.getScheme).exists(_.equalsIgnoreCase(request.getProtocol()))
        val hostMatches = uri.getHost.equalsIgnoreCase(request.getDomain())

        val portFromUrl = uri.getPort // -1 means not specified
        val portMatches =
          if (portFromUrl == -1) true
          else portFromUrl == request.getPort()

        schemeMatches && hostMatches && portMatches
      }
    } catch {
      case _: Exception => false
    }
  }
}
