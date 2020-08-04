package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

/**
 * Date: 04/08/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilGirafaCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 72
   }

   override fun extractProductsFromCurrentPage() {

      currentDoc = fetchDocument(
         "https://www.girafa.com.br/pesquisa/busca/" +
            "?q=$keywordEncoded" +
            "&max=$pageSize" +
            "&page=$currentPage"
      )

      val items = currentDoc.select(".conteudo-categorias .conteudo_linha_produtos")

      for (it in items) {

         val productUrl = CrawlerUtils.scrapUrl(it, ".preco-link", listOf("href"),"https://", "www.girafa.com.br")
         val internalId = it.selectFirst(".card-interno").attr("data-id")

         saveDataProduct(internalId, internalId, productUrl)
         log(">>> ✅ productId: $internalId")
      }
   }

   override fun hasNextPage(): Boolean {


      val actual = currentDoc.selectFirst(".paginacao ul li .active")?.attr("href")
      val last = currentDoc.select(".paginacao ul li a").last()?.attr("href")

      return actual != last
   }
}
