package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.MathUtils
import org.apache.http.impl.cookie.BasicClientCookie

class SaopauloSupermercadospaguemenosCrawler(session: Session?) : CrawlerRankingKeywords(session) {

   private val zipCode: String = getZipCode()

   init {
      pageSize = 40
   }

   fun getZipCode(): String {
      return session.options.optString("zip_code")
   }

   override fun processBeforeFetch() {
      if (zipCode != "") {
         val cookie = BasicClientCookie("zipcode", zipCode)
         cookie.domain = "www.superpaguemenos.com.br"
         cookie.path = "/"
         cookies.add(cookie)
      }
   }

   override fun extractProductsFromCurrentPage() {
      val url = "https://www.superpaguemenos.com.br/${keywordEncoded.replace("+", "%20")}/?p=$currentPage"

      currentDoc = fetchDocument(url)
      for (element in currentDoc.select(".item-product")) {
         val internalId = element.attr("data-id")?.replace("sku_", "")
         val productUrl = CrawlerUtils.scrapUrl(element, "h2.title a", "href", "https", "www.superpaguemenos.com.br")
         val price = MathUtils.parseInt(element.selectFirst("meta[itemprop=price]")?.attr("content")?.replace(".", ""))
         val imageUrl = element.selectFirst("img[itemprop=image]")?.attr("content")
         val name = element.selectFirst("h2.title a span")?.html()
         val isAvailable = price != null;

         val product = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setAvailability(isAvailable)
            .setPriceInCents(price)
            .setName(name)
            .setImageUrl(imageUrl)
            .build();

         saveDataProduct(product)
         log("internalId $internalId - url $productUrl")
      }
   }

   override fun hasNextPage() = currentDoc.selectFirst(".next") != null
}
