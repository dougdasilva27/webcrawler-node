package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class BrasilNutricaoatevoceCrawler(session: Session) : CrawlerRankingKeywords(session) {

   override fun extractProductsFromCurrentPage() {
      log("Página $currentPage")
      val keyword = this.keywordWithoutAccents.replace(" ", "%20");
      val url = "https://www.nutricaoatevoce.com.br/catalogsearch/result/?q=$keyword"
      log("Link onde são feitos os crawlers: $url")
      currentDoc = fetchDocument(url)

      val products = currentDoc.select(".product-items > li")
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            totalProducts = products.size;
            log("Total da busca: $totalProducts")
         }

         for (product in products) {
            val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div.product-item-details > form input[name=product]", "value")
            val productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div.product-item-info > div.image-container > a", "href")

            saveDataProduct(internalId, null, productUrl)
            log("Position: $position - InternalId: $internalId - InternalPid: null - Url: $productUrl")

            if (arrayProducts.size == productsLimit) {
               break
            }
         }
      } else {
         result = false
         log("Keyword sem resultado!")
      }

      log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados")
   }

   override fun hasNextPage(): Boolean {
      return false;
   }
}
