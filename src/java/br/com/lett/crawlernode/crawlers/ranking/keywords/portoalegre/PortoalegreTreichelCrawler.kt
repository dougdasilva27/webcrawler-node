package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject

class PortoalegreTreichelCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 30
   }

   override fun extractProductsFromCurrentPage() {
      val json = dataFetcher.get(
         session, RequestBuilder.create()
            .setUrl("https://delivery.atacadotreichel.com.br/api/buscaAmigavel?descricao=$keywordEncoded")
            .build()
      ).body?.toJson()!!

      for (elem in json.optJSONArray("Produtos")) {
         if (elem is JSONObject) {
            val productUrl = "https://delivery.atacadotreichel.com.br/produto/${elem.optString("str_link_produto")}"
            val id = elem.optString("id_produto")
            log("Url $productUrl - internalId - $id")
            saveDataProduct(id, null, productUrl)
         }
      }
   }

   override fun checkIfHasNextPage(): Boolean {
      return (arrayProducts.size % pageSize - currentPage) < 0
   }
}
