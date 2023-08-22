package one.jpro.routing

import java.net.URL

import com.jpro.webapi.{HTMLView, SVGView, WebAPI}
import simplefx.all._
import simplefx.core._

class ParallaxView(imgurl: URL) extends StackPane { THIS =>

  @Bind var image: Image = new Image(imgurl.toString)

  private var webAPI: WebAPI = null

  val id = "mynodeid_" + random[Int]
  val patternid = "mypatternid_" + random[Int]
  val imgid = "myimgid_" + random[Int]

  def relativeHeight = 100

  if(!WebAPI.isBrowser) {
    this <++ new ImageView(image) {
      fitWH       <-- (THIS.width, 0)
      clip        <-- new Rectangle { this.wh <-- THIS.wh }
      preserveRatio = true
      managed       = false
    }
  }

  if(WebAPI.isBrowser) (updated {
    onceWhen(scene != null) --> {
      webAPI = WebAPI.getWebAPI(this.scene)

      this <++ new SVGView {
        @Bind var content: String = contentProperty().toBindable

        content <-- {
          s"""<defs>
             |  <pattern id="$patternid" patternUnits="userSpaceOnUse" width="100%" height="100%">
             |            <image id="$imgid" xlink:href="${webAPI.createPublicFile(imgurl)}"
             |            x="0" y="-${relativeHeight}%"
             |            preserveAspectRatio="xMidYMid slice"
             |            width="100%" height="${100 + relativeHeight}%" />
             |  </pattern>
             |</defs>
             |
             |<rect id="$id" width="100%" height="100%" fill="url(#$patternid)"></rect>
             |
             |""".stripMargin
        }

        // https://jsfiddle.net/nqaq4vz1/
        /**
          *   console.log("scrollPosition: " + scrollPosition);
          *   console.log("nodeHeight: " + nodeHeight);
          *   console.log("screenHeight: " + screenHeight);
          *   console.log("nodeY: " + nodeY);
          *
          */
        webAPI.executeScript(
          s"""(function() {
            |var update = (function() {
            |   var x = document.getElementById("${id}");
            |   if(x != null) {
            |   var img = document.getElementById("${imgid}");
            |   var nodeHeight = x.getBoundingClientRect().height;
            |   var nodeY = x.getBoundingClientRect().top;
            |   var screenHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
            |   var doc = document.documentElement;
            |   var scrollPosition = (window.pageYOffset || doc.scrollTop)  - (doc.clientTop || 0);
            |
            |   var min = -nodeHeight;
            |   var max = screenHeight;
            |   var dif = max - min;
            |   var perc = (nodeY - min) / dif
            |   var perc2 = Math.max(0.0, Math.min(perc,1.0))
            |
            |   img.setAttribute("y", "" + (-perc2 * ${relativeHeight}) + "%");
            |   }
            |});
            |window.addEventListener("scroll", update);
            |update();
            |
            |})();
          """.stripMargin)
      }

    }
  })


}
