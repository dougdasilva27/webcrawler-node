package br.com.lett.crawlernode.crawlers.extractionutils.ranking

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc

/**
 * Date: 28/01/21
 *
 * @author Fellype Layunne
 *
 */
class BodegamixCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      fetchMode = FetchMode.FETCHER
      pageSize = 30
   }

   private fun fetchProducts() {

      val url = "https://www.bodegamix.com.br/Catalog/LoadMoreProducts?query=$keywordEncoded&pageIndex=${currentPage - 1}&orderBy=0&_=${System.currentTimeMillis()}"

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .build()

      val response = dataFetcher.get(session, request)

      currentDoc = response.body.toDoc()
   }

   override fun extractProductsFromCurrentPage() {

      fetchProducts()

      for (elem in currentDoc.select(".item-box")) {

         val internalId = elem.select(".product-item").attr("data-productid")
         val productUrl = "https://www.bodegamix.com.br${elem.select("a").attr("href")}"
         saveDataProduct(internalId, internalId, productUrl)
         log("Position: $position - InternalId: $internalId - Url: $productUrl")
      }

      log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} +  produtos crawleados")
   }

   override fun checkIfHasNextPage(): Boolean {
      return arrayProducts.size / currentPage >= pageSize
   }

}
