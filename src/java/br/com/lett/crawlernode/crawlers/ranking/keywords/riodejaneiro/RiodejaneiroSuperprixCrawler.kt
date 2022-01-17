package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro

import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.apache.http.impl.cookie.BasicClientCookie
import org.jsoup.nodes.Element
import java.util.*

class RiodejaneiroSuperprixCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private val home = "https://www.superprix.com.br/"

   val token: String by lazy {
      fetchDocument(home).selectFirst("li[layout]").attr("layout")
   }

   init {
      pageSize = 15
   }

   override fun extractProductsFromCurrentPage() {
      val url = home + "buscapagina?ft=${keywordWithoutAccents.replace(" ", "+")}" +
         "&PS=15&sl=$token&cc=15&sm=0&PageNumber=$currentPage"
      val fetchDocument = fetchDocument(url, cookies)

      fetchDocument.select(".prateleira li[layout]")?.forEach {
         val internalId = it.selectFirst(".buy-button-normal")?.attr("id")
         val internalPid = it.selectFirst(".avaliacao div")?.attr("id")?.split("-")?.last()
         val productUrl = it.selectFirst("a")?.attr("href")
         val name = CrawlerUtils.scrapStringSimpleInfo(it, ".data > h3 > a", true);
         val imgUrl = CrawlerUtils.scrapSimplePrimaryImage(it, ".productImage img", Arrays.asList("src"), "https", "")
         val price = CrawlerUtils.scrapPriceInCentsFromHtml(it, ".newPrice em", null, false, ',', session, 0)

         val isAvailable = price != 0

         val productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setImageUrl(imgUrl)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build()

         saveDataProduct(productRanking)
      }
   }

   override fun checkIfHasNextPage(): Boolean {
      return (arrayProducts.size % pageSize - currentPage) < 0
   }
}
