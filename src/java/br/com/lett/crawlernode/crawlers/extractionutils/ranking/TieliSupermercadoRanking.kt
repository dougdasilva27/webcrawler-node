package br.com.lett.crawlernode.crawlers.extractionutils.ranking

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc

abstract class TieliSupermercadoRanking(session: Session) : CrawlerRankingKeywords(session) {

   protected abstract val emp: String

   private val searchUrl = "http://compras.tieli.com.br:38999/KiProcesso/CompraOnLine2.jsp?emp=${emp}&a=0"

   override fun processBeforeFetch() {
      cookies.addAll(fetchCookies("http://compras.tieli.com.br:38999/KiProcesso/CompraOnLine1.jsp?dp=PC"))
   }

   override fun extractProductsFromCurrentPage() {
      val headers = mutableMapOf(
         "Content-Type" to "application/x-www-form-urlencoded",
         "Accept" to "*/*",
         "Cookie" to "JSESSIONID=${cookies.first { it.name == "JSESSIONID" }.value}; __cfduid=d33f084f0b0c5802e097effe60c91b2101617986855"
      )
      val request = RequestBuilder.create()
         .setUrl(searchUrl)
         .setCookies(cookies)
         .setHeaders(headers)
         .setPayload("valor=$keywordEncoded").build()
      currentDoc = dataFetcher.post(session, request).body?.toDoc()

      currentDoc.select(".tbProduto").forEach { doc ->
         val internalId = doc.selectFirst(".tbProduto b").text().split("-").last()
         saveDataProduct(internalId, null, "http://compras.tieli.com.br:38999/$internalId")
      }
   }

   override fun checkIfHasNextPage(): Boolean {
      return false
   }
}
