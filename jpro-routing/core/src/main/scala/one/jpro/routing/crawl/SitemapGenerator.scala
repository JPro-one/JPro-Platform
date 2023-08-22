package one.jpro.routing.crawl

import AppCrawler.CrawlReportApp

import javax.xml.transform.OutputKeys

object SitemapGenerator {
  def createSitemap(prefix: String, report: CrawlReportApp): String = {
    import javax.xml.parsers.DocumentBuilder
    import javax.xml.parsers.DocumentBuilderFactory
    import javax.xml.transform.TransformerFactory
    import javax.xml.transform.dom.DOMSource
    import javax.xml.transform.stream.StreamResult

    val docFactory = DocumentBuilderFactory.newInstance
    val docBuilder = docFactory.newDocumentBuilder
    val doc = docBuilder.newDocument
    val urlset = doc.createElement("urlset")
    urlset.setAttribute("xmlns","http://www.sitemaps.org/schemas/sitemap/0.9")
    urlset.setAttribute("xmlns:image","http://www.google.com/schemas/sitemap-image/1.1")
    doc.appendChild(urlset)

    report.reports.map { page =>
      val child1 = doc.createElement("url")
      val loc = doc.createElement("loc")
      loc.setTextContent(prefix + page.path)

      child1.appendChild(loc)

      page.pictures/*.filter(_.url.startsWith("http"))*/.map { img =>
        val image = doc.createElement("image:image")
        val imageloc = doc.createElement("image:loc")
        if(img.url.startsWith("http")) {
          imageloc.setTextContent(img.url)
        } else {
          imageloc.setTextContent(prefix + img.url)
        }
        image.appendChild(imageloc)
        child1.appendChild(image)
      }
      urlset.appendChild(child1)
    }

    val transformerFactory = TransformerFactory.newInstance
    val transformer = transformerFactory.newTransformer
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    val source = new DOMSource(doc)

    import javax.xml.transform.stream.StreamResult
    import java.io.StringWriter
    val writer = new StringWriter
    val result = new StreamResult(writer)
    transformer.transform(source, result)
    writer.toString
  }

}
