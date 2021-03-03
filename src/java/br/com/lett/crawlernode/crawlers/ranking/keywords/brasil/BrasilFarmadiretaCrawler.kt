package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toDoc
import org.jsoup.nodes.Document

class BrasilFarmadiretaCrawler (session: Session) : CrawlerRankingKeywords(session) {

   private fun fetchProducts(url: String): Document {

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toDoc() ?: Document(url)
   }

   override fun extractProductsFromCurrentPage() {
      pageSize = 12
      val url = "https://www.farmadireta.com.br/busca/?Pagina=${currentPage}&q=${keywordEncoded}"

      log("Página $currentPage")
      log("Link onde são feitos os crawlers: $url")
      this.currentDoc = fetchProducts(url)

      val products = currentDoc.select(".lista-prod > div:not(.clearfix)")

      if (!products.isEmpty()) {
         for (product in products) {
            val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "h3.dgf-titulo>a", "idimpression")
            val productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "h3.dgf-titulo>a", "href")

            saveDataProduct(internalId, null, productUrl)

            log("Position: $position - InternalId: $internalId - InternalPid: $internalId - Url: $productUrl")

            if (arrayProducts.size == productsLimit) {
               break
            }
         }
      } else {
         log("Keyword returned no results!")
      }

      log("Page $currentPage crawled. - ${arrayProducts.size} crawled products until now.")
   }

   override fun hasNextPage(): Boolean {
      return currentDoc.select("a.number-pagination")?.last()?.text()?.toInt() != currentPage
   }

}
