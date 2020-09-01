package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.int
import org.jsoup.nodes.Element

class BrasilSanmichelCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 12
   }

   override fun extractProductsFromCurrentPage() {
      val url = "https://www.meusanmichel.com.br/index.php?page=pesquisa&loja=0&jornal=0&busca=$keywordEncoded&f=&o=&g=&qtp=&p=$currentPage"
      currentDoc = fetchDocument(url)

      if (totalProducts == 0) {
         setTotalProducts()
      }
      for (elem: Element in currentDoc.select(".product-item")) {
         val internalId = elem.selectFirst("a[name]")?.attr("name")?.substringAfter("prod")
         val productUrl = "https://www.meusanmichel.com.br/${elem.selectFirst(".title a").attr("href")}"
         saveDataProduct(internalId, null, productUrl)
         log("InternalId: $internalId - Url: $productUrl")
      }
   }

   override fun setTotalProducts() {
      totalProducts = currentDoc.select(".result-counter span")?.getOrNull(1)?.text()?.substringAfter("a")?.int() ?: 0
   }
}
