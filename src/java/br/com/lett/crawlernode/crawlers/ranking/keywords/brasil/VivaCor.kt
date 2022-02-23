package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class VivaCor(session: Session) : CrawlerRankingKeywords(session) {
   override fun extractProductsFromCurrentPage() {
      pageSize = 24
      val url = "https://www.vivacortintas.com/busca?search=$keywordEncoded&page=$currentPage"
      currentDoc = fetchDocument(url)
      val products = currentDoc.select(".product-layout.product-grid")
      for (e in products) {
         val internalPid = e.selectFirst("meta").attr("content")
         val productUrl = e.selectFirst("a").attr("href")

         val name = CrawlerUtils.scrapStringSimpleInfo(e, ".product_list_informations > h3 > a > span", true)
         val imageUrl= CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".image > a > link", "href")
         val price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".caption > span > p", null, true, ',', session, 0)
         val isAvailable = price != 0

         val productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalPid)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .setImageUrl(imageUrl)
            .build()
         saveDataProduct(productRanking)
      }
   }

   override fun hasNextPage(): Boolean {
      return (arrayProducts.size % pageSize - currentPage) < 0
   }
}
