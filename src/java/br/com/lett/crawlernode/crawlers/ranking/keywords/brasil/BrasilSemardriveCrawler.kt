package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.HashMap

/**
 * Date: 22/10/20
 *
 * @author Fellype Layunne
 *
 */
abstract class BrasilSemardriveCrawler(session: Session) : CrawlerRankingKeywords(session) {

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

   fun fetchDocument(): Document {
      val url = "${HOME_PAGE}?keywords=${keywordEncoded}&page=${currentPage}&utf8=%E2%9C%93"

      val token = cookies.first {
         it.name == "guest_token"
      }?.value ?: ""

      val headers: MutableMap<String, String> = HashMap()

      headers["cookie"] = "guest_token=${token};"
      headers["authority"] = "drive.gruposemar.com.br"
      headers["accept"] = "text/html, application/xhtml+xml"
      headers["user-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36"

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return Jsoup.parse(response?.body ?: "")
   }

   override fun extractProductsFromCurrentPage() {

      if (currentPage == 1) {
         handleCookies()
      }

      currentDoc = fetchDocument()

      val products = currentDoc.select("#product-results .product")

      for (product in products) {

         val internalId = product.selectFirst(".buttons input[name=variant_id]")?.attr("value")

         val path = product.selectFirst(".text a")?.attr("href") ?: ""

         val productUrl = "$HOME_PAGE$path"

         saveDataProduct(internalId, internalId, productUrl)
         log(">>> productId: $internalId || url: $productUrl || name: ${product.selectFirst(".text .product-title")?.text() ?: ""}")
      }
   }

   override fun hasNextPage(): Boolean {
      return currentDoc.selectFirst("#products-paginator a") != null
   }
}
