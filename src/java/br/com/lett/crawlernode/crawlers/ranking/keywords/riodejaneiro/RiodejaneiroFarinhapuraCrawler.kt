package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc
import br.com.lett.crawlernode.util.toJson

class RiodejaneiroFarinhapuraCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private var next = false
   override fun extractProductsFromCurrentPage() {

      val url = "https://loja.farinhapura.com.br/busca?q=" + this.keywordEncoded

      currentDoc = fetchDocument(url)


      currentDoc.select(".products-list  .list .product-template")?.forEach { doc ->
         val url = doc.selectFirst(".texts a").attr("href")
         val internalPid = doc.selectFirst("input[name=id]")?.`val`()
         val list = url.split("/")
         val internalId = list[list.indexOf("produto") + 1]
         saveDataProduct(internalId, internalPid, url)
         log("Position: ${this.position} - InternalId: $internalId - InternalPid: $internalPid - Url: $url")
      }
   }

   override fun hasNextPage(): Boolean = next
}
