package br.com.lett.crawlernode.crawlers.corecontent.peru

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewImpl
import org.json.JSONObject
import org.jsoup.nodes.Document

class PeruPlazaveaCrawler(session: Session) : VTEXNewImpl(session) {

   override fun getHomePage(): String {
      return session.options.optString("homePage")
   }

   override fun getMainSellersNames(): List<String> {
      return session.options?.optJSONArray("sellers")?.toList()
         ?.map { obj: Any -> obj.toString() } ?: listOf()
   }

   override fun scrapDescription(doc: Document?, productJson: JSONObject): String? {
      val description = productJson.optString("Descripción del producto")
      return if (description.isEmpty()) {
         productJson.optString("metaTagDescription")
      } else description
   }
}
