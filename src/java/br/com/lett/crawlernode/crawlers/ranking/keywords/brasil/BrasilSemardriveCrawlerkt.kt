package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toJson
import java.util.HashMap

/**
 * Date: 22/10/20
 *
 * @author Fellype Layunne
 *
 */
abstract class BrasilSemardriveCrawlerkt(session: Session) : CrawlerRankingKeywords(session) {

   companion object {
      const val HOME_PAGE = "https://drive.gruposemar.com.br/products"
   }

   init {
      fetchMode = FetchMode.APACHE
   }

   abstract fun getZipCode(): String

   fun handleCookies() {
      val url = "https://drive.gruposemar.com.br/current_stock"

      val headers: MutableMap<String, String> = HashMap()
      headers["content-type"] = "application/x-www-form-urlencoded; charset=UTF-8"
      headers["origin"] = "https://drive.gruposemar.com.br"
      headers["accept"] = "*/*;q=0.5, text/javascript, application/javascript, application/ecmascript, application/x-ecmascript"

      val payload = "utf8=%E2%9C%93" +
         "&_method=put" +
         "&order%5Bshipping_mode%5D=delivery" +
         "&order%5Bship_address_attributes%5D%5Btemporary%5D=true" +
         "&order%5Bship_address_attributes%5D%5Bzipcode%5D=${getZipCode()}"

      val response = FetcherDataFetcher().post(
         session, Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .build()
      )
      cookies = response.cookies
   }

   fun fetchDocument(): String {
      val url = "https://www.semarentrega.com.br/ccstoreui/v1/search?suppressResults=false&searchType=simple&No=1&Nrpp=24&Ntt=${keywordEncoded}&page=${currentPage-1}"

      val headers: MutableMap<String, String> = HashMap()

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body//return Jsoup.parse(response?.body ?: "")
   }

   override fun extractProductsFromCurrentPage() {


      val body = fetchDocument()
      val obj = body.toJson()
      val products = obj.getJSONObject("resultsList").getJSONArray("records")

      for (p in products) {
         val poduct = JSONUtils.getValueRecursive(p,"attributes,product.repositoryId,0", ",", String.javaClass, "")
//         val internalId = product
//
//         val path = product.selectFirst(".text a")?.attr("href") ?: ""
//
//         val productUrl = "$HOME_PAGE$path"
//
//         saveDataProduct(internalId, internalId, productUrl)
//         log(">>> productId: $internalId || url: $productUrl || name: ${product.selectFirst(".text .product-title")?.text() ?: ""}")
      }
   }

   override fun hasNextPage(): Boolean {
      return currentDoc.selectFirst("#products-paginator a") != null
   }
}
