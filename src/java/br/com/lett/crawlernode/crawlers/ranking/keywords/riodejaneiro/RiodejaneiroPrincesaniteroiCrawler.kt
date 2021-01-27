package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toDoc
import org.jsoup.nodes.Document

/**
 * Date: 27/01/21
 *
 * @author Fellype Layunne
 *
 */

class RiodejaneiroPrincesaniteroiCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      fetchMode = FetchMode.FETCHER
      pageSize = 12
   }


   private fun getRegion(): String {
      return br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro.RiodejaneiroPrincesaniteroiCrawler.REGION
   }

   override fun processBeforeFetch() {
      cookies = br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro.RiodejaneiroPrincesaniteroiCrawler.getCookiesInRegion(getRegion(), dataFetcher, session)
   }

   private fun fetchProducts(): Document {

      val current = (currentPage - 1) * pageSize
      val url = "https://www.princesasupermercados.com.br/produtos/pagina" +
         "?paginaAtual=$current" +
         "&numPorPagina=$pageSize" +
         "&pf.ordenacao=RELEVANCE" +
         "&pf.texto=${keywordEncoded}"

      val headers = HashMap<String, String>()
      headers["Accept"] = "*/*"
      headers["Cookie"] = CommonMethods.cookiesToString(cookies)

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toDoc() ?: Document(session.originalURL)
   }

   override fun extractProductsFromCurrentPage() {

      currentDoc = fetchProducts()

      val products = currentDoc.select(".prod-box")

      for (productDoc in products) {

         val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(productDoc, ".prod-box-amount input[name=\"product.id\"][value]", "value")

         val productUrlPath = CrawlerUtils.scrapStringSimpleInfoByAttribute(productDoc, ".prod-box-title a[href]", "href")

         val productUrl = "https://www.princesasupermercados.com.br$productUrlPath"
         saveDataProduct(internalId, null, productUrl)
         log("Position: $position - internalId: $internalId - internalPid null - url: $productUrl")
      }
   }

   override fun hasNextPage(): Boolean {
      return CrawlerUtils.scrapStringSimpleInfo(currentDoc, ".paginator-button .paginator-button-numbers", false) != null
   }
}
