package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords

/**
 * Date: 21/07/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilIngredientesonlineCrawler(session: Session) : CrawlerRankingKeywords(session) {

    init {
        pageSize = 18
    }

    override fun extractProductsFromCurrentPage() {

        currentDoc = fetchDocument(
                "https://www.ingredientesonline.com.br/catalogsearch/result/" +
                        "?q=$keywordEncoded" +
                        "&p=$currentPage"
        )

        val items = currentDoc.select(".item .suporte")

        for (it in items) {

            val productUrl = it.selectFirst(".product-image-wrapper a").attr("href")
            val internalId = it.selectFirst(".bt-add button").attr("data-id")

            saveDataProduct(internalId, null, productUrl)
            log(">>> ✅ productId: $internalId")
        }
    }

    override fun hasNextPage(): Boolean {

        return currentDoc.select(".pager li .i-next").isNotEmpty()
    }
}
