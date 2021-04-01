package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject

class BrasilLojasmmCrawler(session: Session) : CrawlerRankingKeywords(session) {

   companion object {
      private const val auth =
         "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6bnVsbCwiYWRtaW4iOnRydWUsImp0aSI6IjBkYmQ4ZWMyNTZhN2ZkNDdjZGY2NmNlN2M1NmI1YjVmNDI4MmU5MDI1MmM5NjllMzJlNWM5ZjJhNWJlYWEyY2EiLCJpYXQiOjE2MDE2NjYxNjcsImV4cCI6MTY1MTI1OTc2NywiZW1haWwiOiJldmVydG9uQHByZWNvZGUuY29tLmJyIiwiZW1wcmVzYSI6bnVsbCwic2NoZW1hIjoiTG9qYXNtbSIsImlkU2NoZW1hIjo0LCJpZFNlbGxlciI6IjExIiwiaWRVc2VyIjoxfQ==.mWjRUrIGznvcrZgpfL0rZsGs+hUA5VJ2uZQYqBmsvWg="
      private val headers: MutableMap<String, String> = mutableMapOf(
         "Authorization" to auth
      )
   }

   init {
      pageSize = 15
   }

   override fun extractProductsFromCurrentPage() {
      val url = "https://www.allfront.com.br/api/busca/$keywordEncoded?offset=${(currentPage - 1) * pageSize}"
      log("Link onde são feitos os crawlers: $url")

      val req = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build()
      val json = dataFetcher.get(session, req)?.body?.toJson() ?: throw IllegalStateException("Not possible to access")
      val products = json.optJSONArray("products")
      for (e in products) {
         if (e is JSONObject) {
            val internalPid = e.get("id").toString()
            val urlProduct = "https://www.lojasmm.com/" + e.getString("link")
            saveDataProduct(null, internalPid, urlProduct)
         }
      }
   }

   override fun hasNextPage(): Boolean {
      return ((arrayProducts.size % pageSize - currentPage) < 0)
   }
}
