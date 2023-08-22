package one.jpro.routing.dev

import simplefx.core._
import simplefx.all._
import simplefx.experimental._
import org.scenicview.ScenicView
import one.jpro.routing.RouteUtils
import one.jpro.routing.Filter
import fr.brouillard.oss.cssfx.CSSFX
import one.jpro.routing.LinkUtil
import one.jpro.routing.filter.container.ContainerFilter
import com.jpro.webapi.WebAPI
import de.sandec.jmemorybuddy.JMemoryBuddyLive
import org.kordamp.ikonli.javafx.FontIcon

object DevFilter {

  object DevFilterContainerFactory extends RouteUtils.SFXContainerFactory {

    override def isContainer(x: Node): Boolean = x.isInstanceOf[MyContainer]
    override def createContainer() = new MyContainer
    class MyContainer extends VBox with Container { CONTAINER =>
      stylesheets <++ "/one/jpro/routing/dev/devfilter.css"

      styleClass <++ "devfilter-vbox"
      override def toString(): String = s"DevFilter(content=$content)"

      @Bind var report:JMemoryBuddyLive.Report = JMemoryBuddyLive.getReport()
      request --> updateReport
      def updateReport(): Unit = {
        println("Calling GC (DevFilter)")
        System.gc()
        report = JMemoryBuddyLive.getReport()
      }

      this <++ new HBox {
        styleClass <++ "devfilter-hbox"
        this <++ new Button() {
          styleClass ::= "devfilter-icon-button"
          graphic = new FontIcon("eva-arrow-back")
          onAction --> {
            LinkUtil.goBack(this)
          }
        }
        this <++ new Button() {
          styleClass ::= "devfilter-icon-button"
          graphic = new FontIcon("eva-arrow-forward")
          onAction --> {
            LinkUtil.goForward(this)
          }
        }
        this <++ new Button() {
          styleClass ::= "devfilter-icon-button"
          graphic = new FontIcon("ion4-ios-refresh")
          onAction --> {
            LinkUtil.refresh(this)
          }
        }

        this <++ new TextField {
          request --> {
            if(request != null) {
              this.text = request.path
            }
          }
          onAction --> {
            LinkUtil.gotoPage(this,getText())
          }
        }
        this <++ new Button("Scenic View") {
            onAction --> {
                if(WebAPI.isBrowser) {
                    val stage = new Stage()
                    WebAPI.getWebAPI(CONTAINER.getScene()).openStageAsPopup(stage)
                    ScenicView.show(content.getScene().getRoot, stage)
                } else {
                    ScenicView.show(this.scene)
                }
            }
        }
        this <++ new Label {
          text <-- ("Pages uncollected: " + report.uncollectedEntries.size())
          onClick --> {e => ???}
        }
        this <++ new Button("Force GC") {
          onAction --> {
            updateReport()
            in(1 s) --> updateReport()
          }
        }
        this <++ new Label {
          def layouts: Option[Int] = if(scene != null) {
            Some(LinkUtil.getSessionManager(this).webApp.getLayoutCounter())
          } else None
          text <-- ("LayoutCounter: " + layouts.map(_.toString).getOrElse("-"))
        }
        //this <++ new Label() {
        //  text <-- (if(request == null) "-" else "request: " + request)
        //}
      }
      this <++ new StackPane {
        pickOnBounds = false
        javafx.scene.layout.VBox.setVgrow(this, Priority.ALWAYS)
        children <-- (if(content != null) List(content) else Nil)
      }
    }
  }

  def create(): Filter = {
    CSSFX.start()
    ContainerFilter.create(DevFilterContainerFactory)
  }
}
