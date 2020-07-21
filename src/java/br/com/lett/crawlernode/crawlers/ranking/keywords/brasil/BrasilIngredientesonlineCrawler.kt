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

    override fun extractProductsFromCurrentPage() {
        currentDoc = fetchDocument(
                "https://www.ingredientesonline.com.br/catalogsearch/result/" +
                        "?q=$keywordEncoded" +
                        "&p=$currentPage"
        )
    }
}
