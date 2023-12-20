package one.jpro.platform.routing

import javafx.scene.Node
import one.jpro.platform.routing.LinkUtil.isValidLink
import org.slf4j.{Logger, LoggerFactory}

import java.lang.ref.WeakReference
import java.net.URI
import java.util.{Map => JMap}

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

  def getPath(): String = path

  def getDirectory(): String = directory

  def getOriginalPath(): String = origPath
  def getDomain(): String = directory
  def getQueryParameter(key: String): Option[String] = queryParameters.get(key)

  def getQueryParameterOrElse(key: String, default: String): String = queryParameters.getOrElse(key, default)

  private lazy val immutableJavaMap: JMap[String, String] = {
    import scala.collection.JavaConverters._
    queryParameters.asJava
  }
  def getQueryParameters(): JMap[String,String] = immutableJavaMap
  def getOriginalOldContent(): WeakReference[Node] = origOldContent
  def getOldContent(): WeakReference[Node] = oldContent

  def getQueryParametersScala(): Map[String,String] = queryParameters


  def getPort(): Int = port

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
  def fromString(x: String): Request = {
    if(!isValidLink(x)) {
      logger.warn("Warning - Invalid Link: " + x)
    }
    val uri = new URI(x)
    val rawQuery = uri.getRawQuery
    val query: Map[String,String] = if(rawQuery == null || rawQuery == "") Map() else rawQuery.split("&").map(x => {
      val Array(a,b) = x.split("=")
      a -> b
    }).toMap
    val path = uri.getPath
    val res = Request(x, uri.getScheme, uri.getHost, uri.getPort, path,path,"/", query,wref_null,wref_null)
    res
  }
}
