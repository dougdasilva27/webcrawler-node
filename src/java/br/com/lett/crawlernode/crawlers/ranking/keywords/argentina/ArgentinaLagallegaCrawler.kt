package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toDoc
import org.apache.http.cookie.Cookie
import org.apache.http.impl.cookie.BasicClientCookie
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


class ArgentinaLagallegaCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      fetchMode = FetchMode.FETCHER
      pageSize = 50
   }

   private fun getCookies(): List<Cookie> {
      return br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaLagallegaCrawler.getCookies(dataFetcher, session)
   }

   override fun processBeforeFetch() {

      cookies = getCookies()

      cookies.add(BasicClientCookie("cantP", "$pageSize"))
   }

   private fun fetchProducts(url: String): Document {

      val headers = HashMap<String, String>()

      headers["Cookie"] = CommonMethods.cookiesToString(cookies)
      headers["Accept"] = "*/*"
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36"


      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toDoc() ?: Document(url)
   }

   override fun extractProductsFromCurrentPage() {

      val url1 = "https://www.lagallega.com.ar/Productos.asp?cpoBuscar=${getKeyword()}"
      val url2 = "https://www.lagallega.com.ar/productos.asp?page=${currentPage}&N1=&N2=&N3=&N4="

      currentDoc = if (currentPage == 1) fetchProducts(url1) else fetchProducts(url2)

      val products = currentDoc.select(".listaProds li")

      for (productDoc in products) {

         val internalId = scrapInternalId(productDoc)

         val productUrl = "https://www.lagallega.com.ar/carrito.asp?Pr=${internalId}&P="
         saveDataProduct(internalId, null, productUrl)
         log("Position: $position - internalId: $internalId - internalPid null - url: $productUrl")
      }
   }

   private fun scrapInternalId(doc: Element): String? {

      val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".InfoProd > div[id*=P]", "id")

      if (internalId != null && internalId.isNotEmpty()) {
         return internalId.substring(1)
      }

      return null
   }

   private fun getKeyword(): String {
      return keywordWithoutAccents.replace(" ", "@")
   }

   override fun hasNextPage(): Boolean {
      val text = CrawlerUtils.scrapStringSimpleInfo(currentDoc, "table > tbody > tr > .pieBot", false) ?: ""
      if (text.isNotEmpty()) {
         val split = text.split("de")

         return (split.size > 1) && (split[0].trim() != split[1].trim())
      }

      return false
   }
}
