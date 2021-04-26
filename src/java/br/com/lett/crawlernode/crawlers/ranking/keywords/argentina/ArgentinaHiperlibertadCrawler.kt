package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.JSONUtils
import org.json.JSONArray
import org.json.JSONObject

class ArgentinaHiperlibertadCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 24
   }

   private fun getProducts(): JSONArray {
      val start = (currentPage - 1) * pageSize
      val end = currentPage * pageSize - 1
      val url = "https://www.hiperlibertad.com.ar/api/catalog_system/pub/products/search/busca?O=OrderByTopSaleDESC" +
         "&_from=$start" +
         "&_to=$end" +
         "&ft=${keywordWithoutAccents.replace("""\s""".toRegex(), "%20")}" +
         "&sc=1"
      val response = dataFetcher.get(session, Request.RequestBuilder.create().setUrl(url).build())
      return JSONUtils.stringToJsonArray(response.body)
   }

   override fun extractProductsFromCurrentPage() {
      val products = getProducts()
      for (product in products) {
         if (product is JSONObject) {
              val internalPid = product.optString("productId")
              val productUrl = product.optString("link")
              saveDataProduct(null, internalPid, productUrl)
         }
      }
   }

   override fun hasNextPage(): Boolean {
      return (arrayProducts.size % pageSize - currentPage) < 0
   }
}
