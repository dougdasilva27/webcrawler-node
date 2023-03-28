package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldNewImpl
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toDoc
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.util.*

class BrasilDrogariaSaoPauloCrawler(session: Session) : VTEXOldNewImpl(session) {

   override fun fetch(): Document {
      val headers = HashMap<String, String>()
      headers["accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
      headers["accept-language"] = "en-US,en;q=0.5"

      val request = Request.RequestBuilder.create()
         .setUrl(session.originalURL)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toDoc() ?: Document(session.originalURL)
   }

   override fun crawlProductApi(internalPid: String?, parameters: String?): JSONObject {
      var productApi = JSONObject()
      val headers: MutableMap<String, String> = HashMap()
      headers["accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
      headers["accept-language"] = "en-US,en;q=0.5"

      val url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + (parameters ?: "")

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setSendUserAgent(true)
         .build()
      val array = CrawlerUtils.stringToJsonArray(dataFetcher[session, request].body)
      if (!array.isEmpty) {
         productApi = if (array.optJSONObject(0) == null) JSONObject() else array.optJSONObject(0)
      }
      return productApi
   }

   override fun scrapDescription(doc: Document?, productJson: JSONObject?): String? {
      val descriptionFullJson = JSONUtils.getStringValue(productJson, "description")
      val descriptionShortHtml = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".productDescriptionShort p"))
      val description = descriptionShortHtml + ' ' + descriptionFullJson
      return description
   }
}
