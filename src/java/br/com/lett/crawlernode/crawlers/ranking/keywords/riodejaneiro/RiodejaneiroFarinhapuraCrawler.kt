package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc
import br.com.lett.crawlernode.util.toJson
import org.apache.http.HttpHeaders

class RiodejaneiroFarinhapuraCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private var next = false
   override fun extractProductsFromCurrentPage() {

      val request = Request.RequestBuilder.create()
         .setUrl("https://loja.farinhapura.com.br/busca?q=$keywordEncoded")
         .setPayload("offset=${currentPage - 1}&more=1")
         .setHeaders(mapOf(HttpHeaders.CONTENT_TYPE to "application/x-www-form-urlencoded"))
         .build()
      val json = dataFetcher.post(session, request).body.toJson()
      next = json.optBoolean("temMais", false)
      currentDoc = json.getString("produtos")?.toDoc()

      currentDoc.select(".product-template")?.forEach { doc ->
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
