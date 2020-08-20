package br.com.lett.crawlernode.crawlers.ranking.keywords.belgium

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject

class BelgiumColruytCrawler(session: Session) : CrawlerRankingKeywords(session) {
   init {
      pageSize = 21
   }

   override fun extractProductsFromCurrentPage() {
      val url = "https://ecgproductsearchmw.colruyt.be/ecgproductsearchmw/v1/fr/products?searchTerm=$keywordEncoded&placeId=604&client=clp&page=$currentPage&size=$pageSize"
      val json = fetchJSON(url)
      totalProducts = json.optInt("productsFound", 0)
      log("Total de produtos: $totalProducts")
      json.optJSONArray("products")?.forEach { elem ->
         if (elem is JSONObject) {
            val internalId = elem.optString("commercialArticleNumber")
            val internalPid = internalId
            val productUrl = "https://www.colruyt.be/fr/produits/${elem.optString("name").replace(' ', '-')}-$internalId"
            saveDataProduct(internalId, internalPid, url)

            log("Position: $position - InternalId: $internalId - InternalPid: $internalPid - Url: $productUrl")
         }
      }
      log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados")
   }

   fun fetchJSON(url: String): JSONObject {
      val result = dataFetcher.get(
         session, Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(listOf(
            ProxyCollection.BE_OXYLABS,
            ProxyCollection.STORM_RESIDENTIAL_US,
            ProxyCollection.NETNUT_RESIDENTIAL_ES))
         .build()
      )?.body
      return result?.toJson() ?: throw IllegalStateException("It was not possible to fetch $url")
   }
}
