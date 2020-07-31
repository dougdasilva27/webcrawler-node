package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import org.json.JSONObject

class BrasilSodimacCrawler(session: Session?) : CrawlerRankingKeywords(session) {
   
   init {
      pageSize = 28
   }

   override fun extractProductsFromCurrentPage() {
      val url = "https://www.sodimac.com.br/s/search/v1/sobr?q=$keywordEncoded&priceGroup=1018&zone=35745&currentpage=$currentPage"
      val jsonApi = fetchJSONObject(url)

      jsonApi?.optJSONObject("data")?.optJSONArray("results")?.forEach { elem ->
         if (elem is JSONObject) {
            val internal = elem.optString("skuId")
            val productUrl = "https://www.sodimac.com.br/sodimac-br/product/$internal/${elem.optString("displayName")
               .replace("""(,)?\s""".toRegex(), "-").replace(",", "")}/$internal"

            saveDataProduct(internal, null, productUrl)
            log("InternalId: $internal - Url: $productUrl")
         }
      }
   }

   override fun checkIfHasNextPage(): Boolean {
      return (arrayProducts.size % pageSize - currentPage) < 0
   }
}
