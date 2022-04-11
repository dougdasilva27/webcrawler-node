package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toDoc
import org.apache.http.cookie.Cookie
import org.apache.http.impl.cookie.BasicClientCookie
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*

/**
 * Date: 26/01/21
 *
 * @author Fellype Layunne
 *
 */
class ArgentinaLareinaCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      fetchMode = FetchMode.JSOUP
      pageSize = 50
   }

   private fun getCookies(): List<Cookie> {
      return br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaLareinaCrawler.getCookies(dataFetcher, session)
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

      var body = ""

      if (currentPage == 1) {
         val payload = "cpoB=${keywordEncoded}&TM=Bus";

         val request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .setProxyservice(
               listOf(
                  ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
                  ProxyCollection.BUY_HAPROXY,
                  ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
                  ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
               )
            )
            .setPayload(payload)
            .setSendUserAgent(true)
            .build()
         body = dataFetcher.post(session, request).body

      } else {

         val request = Request.RequestBuilder.create()
            .setUrl(url + "?pg=${currentPage}&nl=&TM=Bus&cpoB=${keywordEncoded}")
            .setHeaders(headers)
            .setProxyservice(
               listOf(
                  ProxyCollection.BUY_HAPROXY,
                  ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
                  ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
                  ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
               )
            )
            .setSendUserAgent(false)

            .build()

         body = dataFetcher.get(session, request).body
      }

      return body.toDoc() ?: Document(url)
   }

   override fun extractProductsFromCurrentPage() {

      val url = "https://www.lareinaonline.com.ar/productosnl.asp"

      currentDoc = fetchProducts(url)

      val products = currentDoc.select(".listaProds li")

      for (productDoc in products) {

         val internalId = scrapInternalId(productDoc)
         val name = CrawlerUtils.scrapStringSimpleInfo(productDoc, ".desc", true)
         val price = scrapPrice(productDoc)
         val imageUrl = CrawlerUtils.scrapSimplePrimaryImage(productDoc, ".FotoProd img", Arrays.asList("src"), "https", "www.lareinaonline.com.ar")
         val productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(productDoc, ".FotoProd a", "href"), "https", "www.lareinaonline.com.ar")
         val isAvailable = price != null

         val productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(null)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .setImageUrl(imageUrl)
            .build()

         saveDataProduct(productRanking)
      }
   }

   private fun scrapPrice(productDoc: Element): Int? {
      var price = CrawlerUtils.scrapIntegerFromHtml(productDoc, ".precio .der", null, null, true, false, null)
      if (price == null) {
         price = CrawlerUtils.scrapIntegerFromHtml(productDoc, ".precio", null, null, true, false, null)
      }

      return price
   }

   private fun scrapInternalId(doc: Element): String? {

      val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".InfoProd > div[id]", "id")

      if (internalId != null && internalId.isNotEmpty()) {
         return internalId.substring(1)
      }

      return null
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
