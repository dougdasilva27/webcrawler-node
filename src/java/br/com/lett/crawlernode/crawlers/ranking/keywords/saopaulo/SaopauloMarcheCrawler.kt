package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.crawlers.extractionutils.core.MarcheCrawler
import br.com.lett.crawlernode.util.JSONUtils
import org.jsoup.Jsoup
import kotlin.math.roundToInt

class SaopauloMarcheCrawler(session: Session) : CrawlerRankingKeywords(session) {
   val home = "https://www.marche.com.br"

   override fun processBeforeFetch() {
      this.cookies = MarcheCrawler.getCookies(br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloMarcheCrawler.CEP, FetcherDataFetcher(), session)
   }

   override fun extractProductsFromCurrentPage() {
      val url = "https://www.marche.com.br/search?utf8=%E2%9C%93&query=$keywordEncoded&page=$currentPage"

      val request = RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .build()

      log("Link onde são feitos os crawlers: $url")

      Jsoup.parse(dataFetcher.get(session, request).body)?.let { doc ->
         val productsElements = doc.select("div[data-infinite-scroll='items'] > div[data-json]")
         this.pageSize = productsElements.size

         productsElements.asSequence()
            .map { JSONUtils.stringToJson(it.attr("data-json")) }
            .forEachIndexed { index, jsonProd ->
               val urlProd = home + doc.select("a[class=link]")?.get(index)?.attr("href")
               val internalId = jsonProd.optString("product_id")
               val name = jsonProd.optString("name")
               val price = jsonProd.optDouble("price")*100
               val image =  jsonProd.optString("mini_image")
               val availability = jsonProd.opt("unavailable") != null

               val productRanking = RankingProductBuilder.create()
                  .setUrl(urlProd)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price.roundToInt())
                  .setAvailability(availability)
                  .setImageUrl(image)
                  .build()

               saveDataProduct(productRanking)


            }
      }
      log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados")
   }

   override fun hasNextPage(): Boolean {
      return this.pageSize >= 20
   }
}
