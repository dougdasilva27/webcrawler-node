package br.com.lett.crawlernode.crawlers.ranking.keywords.salvador

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toInt

class SalvadorHiperidealCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 16
   }

   private val token: String? by lazy {
      val doc = fetchDocument("https://www.hiperideal.com.br/$keywordEncoded")
      totalProducts = doc.selectFirst(".resultado-busca-numero .value")?.toInt() ?: 0
      doc.selectFirst("li[layout]")?.attr("layout")
   }

   override fun extractProductsFromCurrentPage() {
      token?.let { key ->
         currentDoc = fetchDocument("https://www.hiperideal.com.br/buscapagina?ft=$keywordEncoded&PS=16&sl=$key&cc=4&sm=0&PageNumber=$currentPage")
         for (element in currentDoc.select(".product--large")) {
            val sku = element.selectFirst(".skuid")?.text()
            val url = element.selectFirst(".product__title a")?.attr("href")
            saveDataProduct(sku, null, url)
            log("Internalid $sku - Url $url")
         }
      }
   }
}
