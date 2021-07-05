package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.exceptions.InternalIdNotFound
import org.jsoup.nodes.Element

/**
 * Date: 21/07/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilIngredientesonlineCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 18
      fetchMode = FetchMode.FETCHER
   }

   override fun extractProductsFromCurrentPage() {

      currentDoc = fetchDocument(
         "https://www.ingredientesonline.com.br/catalogsearch/result/" +
            "?q=${location.replace(" ", "+")}" +
            "&p=$currentPage"
      )

      val items = currentDoc.select(".item .suporte")

      for (element in items) {

         val productUrl = element.selectFirst(".product-image-wrapper a")?.attr("href")
         val internalId = scrapInternal(element)

         saveDataProduct(internalId, internalId, productUrl)
         log(">>> productId: $internalId | productUrl: $productUrl")
      }
   }

   private fun scrapInternal(doc: Element): String {
      val element = doc.selectFirst(".bt-add button")

      var internalId = element?.attr("data-id") ?: ""

      if (internalId.isEmpty()){
         internalId = element?.attr("onclick")?.substringAfter("showOptions")?.substringBefore("',")
            ?.replace("[^0-9]".toRegex(), "") ?: ""
      }
      return internalId
   }

   override fun hasNextPage(): Boolean {

      return currentDoc.select(".pager li .i-next").isNotEmpty()
   }
}
