package br.com.lett.crawlernode.crawlers.ranking.keywords.belgium

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*
import kotlin.collections.HashMap

/**
 * Date: 30/07/20
 *
 * @author Fellype Layunne
 *
 */
class BelgiumDelhaizeCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 20
      fetchMode = FetchMode.JAVANET
   }

   override fun extractProductsFromCurrentPage() {

      val result = fetcherPage()



      val elements = result?.select(".data-item")

      if (elements != null) {
         for (element in elements){
            val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(element,"[data-product-id]","data-product-id")
            val productUrl = "https://www.delhaize.be${CrawlerUtils.scrapStringSimpleInfoByAttribute(element,".ProductHeader a","href")}"

            saveDataProduct(internalId, internalId, productUrl)
            log("Position: " + position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl)
         }


      }
   }

   private fun fetcherPage() : Document? {

      val url = "https://www.delhaize.be/fr-be/shop/search?q="+ this.keywordEncoded + ":relevance&sort=relevance&pageNumber="+(this.currentPage - 1)

      val headers: MutableMap<String, String> = HashMap()

      headers["cookie"] = "groceryCookieLang=fr;"

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.BONANZA_BELGIUM_HAPROXY,ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY))
         .build()

      return Jsoup.parse(dataFetcher.get(session, request).body)
   }

   override fun hasNextPage(): Boolean {
      return true
   }

}
