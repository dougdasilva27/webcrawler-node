package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldNewImpl
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.util.*

class BrasilDrogariaSaoPauloCrawler(session: Session) : VTEXOldNewImpl(session) {
   override fun scrapDescription(doc: Document?, productJson: JSONObject?): String? {
      val descriptionSort = JSONUtils.getStringValue(productJson, "description")
      val descriptionFull = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".productDescription"))
      val description = descriptionSort + ' ' + descriptionFull
      return description
   }
}
