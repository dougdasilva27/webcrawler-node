package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc

class PortugalContinenteCrawler(session: Session?) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 36
   }

   override fun extractProductsFromCurrentPage() {

      val request = RequestBuilder.create()
         .setUrl("https://www.continente.pt/pesquisa/?q=$keywordEncoded&start=" + ((currentPage - 1) * pageSize ))
         .setCookies(cookies)
         .setProxyservice(
            listOf(
               ProxyCollection.BUY,
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES
            )
         )
         .build()

      val doc = dataFetcher.get(session, request)?.body?.toDoc() ?: throw IllegalStateException()

      if (currentPage == 1) {
         val totalPageString = doc.selectFirst("div[data-total-count]")?.attr("data-total-count")
         val totalPage = totalPageString?.toInt()
         if (totalPage != null) {
            totalProducts = totalPage
         }
      }

      val products = doc.select(".row.product-grid .product")

      for (elem in products) {
         val internalId = elem.attr("data-pid")
         val productUrl = elem.selectFirst(".ct-pdp-link > a").attr("href")

         val productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setName("nome")
            .setInternalId(internalId)
            .setPriceInCents(1)
            .setAvailability(true)
            .build()

         saveDataProduct(productRanking)

         if (arrayProducts.size == productsLimit) {
            break;
         }
      }
   }

   override fun checkIfHasNextPage(): Boolean = (arrayProducts.size % pageSize - currentPage) < 0
}
