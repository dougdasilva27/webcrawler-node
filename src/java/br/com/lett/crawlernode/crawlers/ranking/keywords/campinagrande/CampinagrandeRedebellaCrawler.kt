package br.com.lett.crawlernode.crawlers.ranking.keywords.campinagrande

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Element

class CampinagrandeRedebellaCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private val BASE_URL: String = "redebella.com.br"

   override fun extractProductsFromCurrentPage() {
      pageSize = 25
      log("Página $currentPage")

      val url = "https://www.redebella.com.br/" + keywordEncoded + "?_q=" + keywordEncoded + "&map=ft&__pickRuntime=queryData"
      log("Link onde são feitos os crawlers: $url")


      var products: JSONArray? = crawProducts(url)


      if (products != null && !products.isEmpty) {
         if (totalProducts == 0) {
            setTotalProducts()
         }
         for (product in products) {

            val internalId = JSONUtils.getStringValue(product as JSONObject?, "productId")
            val internalPid = internalId
            val url: String = JSONUtils.getStringValue(product, "link")
            val productUrl = CrawlerUtils.completeUrl(url, "https", "www.redebella.com.br")

            saveDataProduct(internalId, internalPid, productUrl)

            log("Position: $position - InternalId: $internalId - InternalPid: $internalPid - Url: $productUrl")
            if (arrayProducts.size == productsLimit) {
               break
            }
         }
      } else {
         result = false
         log("Keyword sem resultado!")
      }
      log("Finalizando Crawler de produtos da página $currentPage até agora ${arrayProducts.size} produtos crawleados")
   }

   private fun crawProducts(url: String): JSONArray? {
      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .build()
      val response = dataFetcher[session, request].body
      val jsonResponse = CrawlerUtils.stringToJson(response)
      val data = JSONUtils.getValueRecursive(jsonResponse, "queryData.0.data", String::class.java);
      val jsonData = CrawlerUtils.stringToJson(data)
      return JSONUtils.getValueRecursive(jsonData, "productSearch.products", JSONArray::class.java)
   }


   private fun extractInternalId(product: Element): String? {
      val fullText = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".list-block .addToCart", "onclick")
      return fullText.split("'").getOrNull(1)
   }
}
