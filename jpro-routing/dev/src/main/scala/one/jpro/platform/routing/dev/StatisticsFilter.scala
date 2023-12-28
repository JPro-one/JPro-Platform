package one.jpro.platform.routing.dev

import com.jpro.webapi.WebAPI
import de.sandec.jmemorybuddy.JMemoryBuddyLive
import fr.brouillard.oss.cssfx.CSSFX
import one.jpro.platform.routing.filter.container.ContainerFilter
import one.jpro.platform.routing.{Filter, LinkUtil, RouteUtils}
import org.kordamp.ikonli.javafx.FontIcon
import org.scenicview.ScenicView
import org.slf4j.{Logger, LoggerFactory}
import simplefx.all._
import simplefx.core._
import simplefx.experimental._
import one.jpro.platform.routing.dev.NodesHelper._

object StatisticsFilter {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  object StatisticsFilterContainerFactory extends RouteUtils.SFXContainerFactory {

    override def isContainer(x: Node): Boolean = x.isInstanceOf[MyContainer]

    override def createContainer() = new MyContainer

    class MyContainer extends VBox with Container {
      CONTAINER =>
      stylesheets <++ DevFilter.getClass.getResource("/one/jpro/platform/routing/dev/statisticsfilter.css").toExternalForm

      styleClass <++ "statisticsfilter-vbox"

      override def toString(): String = s"DevFilter(content=$content)"

      this <++ new HBox {

        styleClass <++ "statisticsfilter-hbox"

        var webAPIData: WebAPIData = null

        if (WebAPI.isBrowser) {
          WebAPI.getWebAPI(this, (webAPI: WebAPI) => {
            webAPIData = new WebAPIData(webAPI)
            setupContent()
          })
        } else {
          setupContent()
        }

        @Bind var loadTimeFX = (0 s)
        @Bind var loadTimeJS = (0 s)

        class WebAPIData(val webAPI: WebAPI) {
          val instanceInfo = webAPI.getInstanceInfo

          @Bind val afk = instanceInfo.afkProperty.toBindable
          @Bind val latency = instanceInfo.latencyProperty.toBindable
          @Bind val background = instanceInfo.backgroundProperty.toBindable
          @Bind val lastUserActionTime = instanceInfo.lastActionTimeProperty.toBindable
          @Bind val dataSent = instanceInfo.dataSentWSProperty.toBindable
          @Bind val dataReceived = instanceInfo.dataReceivedWSProperty.toBindable
          @Bind val nodesCreated = instanceInfo.nodesCreatedProperty.toBindable
          @Bind val nodesSynchronized = instanceInfo.nodesSynchronizedProperty.toBindable
          @Bind val nodesCollected = instanceInfo.nodesCollectedProperty.toBindable

          @Bind var createdBeforeRequest = 0
          @Bind val createdSinceRequest = <--(nodesCreated - createdBeforeRequest)
        }


        def setupContent(): Unit = {
          request --> {
            if (WebAPI.isBrowser) {
              webAPIData.createdBeforeRequest := webAPIData.nodesCreated
            }

            val t1 = systemTime
            nextFrame --> {
              val t2 = systemTime
              val t = t2 - t1
              loadTimeFX := t
              if (WebAPI.isBrowser) {
                webAPIData.webAPI.executeScriptWithListener("1", r => {
                  val t3 = systemTime
                  val t = t3 - t2
                  loadTimeJS := t
                })
              }
            }
          }

          def toFileSizeString(x: Int): (String, String) = {
            val kb = x / 1024
            val mb = kb / 1024
            if (mb > 5) (formatInt(mb), "MB")
            else if (kb > 50) (formatInt(kb), "KB")
            else (formatInt(x), "B")
          }

          def toTimeString(x: Time): (String, String) = {
            val ms = x div millisecond
            (ms.toString, "ms")
          }
          def formatInt(x: Int): String = {
            val s = x.toString
            val l = s.length
            if(l > 3) {
              s.substring(0,l-3) + "." + s.substring(l-3,l)
            } else {
              s
            }
          }

          if(WebAPI.isBrowser) {
            this <++ new StatBox(true) {
              labels <++ new Label("sent: ")
              values <++ new Label() {
                text <-- toFileSizeString(webAPIData.dataSent.toInt)._1
              }
              units <++ new Label() {
                text <-- toFileSizeString(webAPIData.dataSent.toInt)._2
              }

              labels <++ new Label("received: ")
              values <++ new Label() {
                text <-- toFileSizeString(webAPIData.dataReceived.toInt)._1
              }
              units <++ new Label() {
                text <-- toFileSizeString(webAPIData.dataReceived.toInt)._2
              }
            }
            //this <++ new Label() {
            //  text <-- ("Latency: " + webAPIData.latency + " ms")
            //}

            this <++ new StatBox(true) {
              labels <++ new Label("Latency: ")
              values <++ new Label() {
                text <-- ("" + toTimeString(webAPIData.latency * (millisecond))._1)
              }
              units <++ new Label() {
                text <-- ("" + toTimeString(webAPIData.latency * (millisecond))._2)
              }
            }
          }

          this <++ new StatBox(true) {
            labels <++ new Label("loadTimeFX: ")
            values <++ new Label() {
              text <-- ("" + toTimeString(loadTimeFX)._1)
            }
            units <++ new Label() {
              text <-- ("" + toTimeString(loadTimeFX)._2)
            }
            if(WebAPI.isBrowser) {
              labels <++ new Label("loadTimeJS: ")
              values <++ new Label() {
                text <-- ("" + toTimeString(loadTimeJS)._1)
              }
              units <++ new Label() {
                text <-- ("" + toTimeString(loadTimeJS)._2)
              }
            }
          }

          if(WebAPI.isBrowser) {
            this <++ new StatBox {
              labels <++ new Label("nodesSynchronized: ")
              values <++ new Label() {
                text <-- ("" + webAPIData.nodesSynchronized)
              }
              labels <++ new Label("createdSinceRequest: ")
              values <++ new Label() {
                text <-- ("" + webAPIData.createdSinceRequest)
              }
            }
          }
          this <++ new StatBox() {
            labels <++ new Label("Nodes: ")
            values <++ new Label() {
              text <-- ("" + formatInt(CONTAINER.treeSize))
            }
            labels <++ new Label("visible Nodes: ")
            values <++ new Label() {
              text <-- ("" + formatInt(CONTAINER.visibleTreeSize))
            }
          }
        }
      }
      class StatBox(addUnits: Boolean = false) extends HBox {
        styleClass <++ "statisticsfilter-statbox"

        val labels = new VBox {
          styleClass <++ "statisticsfilter-statbox-labels"
        }
        val values = new VBox {
          styleClass <++ "statisticsfilter-statbox-values"
        }
        val units = new VBox {
          styleClass <++ "statisticsfilter-statbox-units"
        }

        this <++ labels
        this <++ values
        if(addUnits) {
          this <++ units
        }
      }
      this <++ new StackPane {
        pickOnBounds = false
        javafx.scene.layout.VBox.setVgrow(this, Priority.ALWAYS)
        children <-- (if (content != null) List(content) else Nil)
      }
    }
  }

  def create(): Filter = {
    CSSFX.start()
    ContainerFilter.create(StatisticsFilterContainerFactory)
  }
}
