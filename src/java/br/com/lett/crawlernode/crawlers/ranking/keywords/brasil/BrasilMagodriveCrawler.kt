package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import org.json.JSONObject

/**
 * Date: 15/09/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilMagodriveCrawler(session: Session) : CrawlerRankingKeywords(session) {
   
   override fun extractProductsFromCurrentPage() {

      val url = "https://www.magodrive.com.br/pesquisa?" +
         "pg=${currentPage}" +
         "&t=${keywordEncoded}"

      currentDoc = fetchDocument(url)

      val products = currentDoc.select(".wd-browsing-grid-list ul li[class]")

      for (product in products) {

         val internalId = product.selectFirst(".btn-original input[name=\"Products[0].SkuID\"]")?.attr("value")
         val internalPid = product.selectFirst(".btn-original input[name=\"Products[0].ProductID\"]")?.attr("value")

         val productPath = product.selectFirst(".btn-original input[name=\"Products[0].Url\"]")?.attr("value")

         if (productPath != null) {
            val productUrl = "https://www.magodrive.com.br${productPath}"

            saveDataProduct(internalId, internalPid, productUrl)
            log(">>> ✅ productId: $internalId path: $productPath")
         }
      }
   }

   override fun hasNextPage(): Boolean {
      return currentDoc.select(".next-page .page-next").isNotEmpty()
   }
}
