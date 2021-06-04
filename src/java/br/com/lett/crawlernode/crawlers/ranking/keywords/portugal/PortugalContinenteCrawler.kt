package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class PortugalContinenteCrawler(session: Session?) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 24
   }

   override fun extractProductsFromCurrentPage() {

      val request = RequestBuilder.create()
         .setUrl("https://www.continente.pt/pesquisa/?q=$keywordEncoded&start=" + ((currentPage - 1) * pageSize ))
         .setCookies(cookies)
         .setProxyservice(
            listOf(
               ProxyCollection.INFATICA_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_ES
            )
         ).build()

      val doc = dataFetcher.get(session, request)?.body?.toDoc() ?: throw IllegalStateException()
      val products = doc.select(".row.product-grid .product")
      for (elem in products) {
         val internalId = elem.attr("data-pid")
         val productUrl = elem.selectFirst(".ct-tile--description").attr("href")
         log("internalId - $internalId - url $productUrl")
         saveDataProduct(internalId, null, productUrl)
      }
   }

   override fun checkIfHasNextPage(): Boolean = (arrayProducts.size % pageSize - currentPage) < 0
}
