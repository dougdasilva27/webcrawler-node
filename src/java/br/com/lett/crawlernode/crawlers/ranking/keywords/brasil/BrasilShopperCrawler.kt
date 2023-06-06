package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import org.json.JSONObject

class BrasilShopperCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private var token: String =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJjdXN0b21lcklkIjoyOTIyNjAsImRldmljZVVVSUQiOiIzYTc1YjdkNy1mMDhmLTQ4ZmEtOGM5Mi04OTliZjNkZmE1Y2IiLCJpYXQiOjE2MjQ2MjMzODl9.KXv2rXCKSkwERiGywoP6sI5HB_mSgp_sdsjN79qq338";


   private fun requestProducts(): JSONObject {

      val url = "https://siteapi.shopper.com.br/catalog/search?query=${keywordEncoded}" + "&page=" + this.currentPage;

      val headers: MutableMap<String, String> = HashMap()

      headers["authorization"] = "Bearer $token"
      headers["accept"] = "application/json, text/plain, */*"
      headers["x-store-id"] = session.options.optString("storeId", "3");

      val request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build()

      return CrawlerUtils.stringToJSONObject(dataFetcher[session, request].body)
   }

   override fun extractProductsFromCurrentPage() {

      val productsApi = requestProducts()

      val products = productsApi.optJSONArray("products")
      for (p in products) {
         val product = p as JSONObject
         val internalId = product.optInt("id").toString()
         val imageUrl = product.optString("image")
         val price = CommonMethods.stringPriceToIntegerPrice(product.optString("price"), ',', 0)
         val name = product.optString("name").toString()
         val isAvailable = price > 0

         val productUrl = "https://shopper.com.br/?id=$internalId"

         val productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setImageUrl(imageUrl)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build()

         saveDataProduct(productRanking)

      }
   }

   override fun hasNextPage(): Boolean {
      return true
   }
}
