package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.models.RankingProduct
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.jsoup.nodes.Element
import java.util.*


class BrasilIngredientesonlineCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 18
      fetchMode = FetchMode.FETCHER
   }

   override fun extractProductsFromCurrentPage() {

      currentDoc = fetchDocument(
         "https://www.ingredientesonline.com.br/catalogsearch/result/" +
            "?q=${location.replace(" ", "+")}" +
            "&p=$currentPage"
      )

      val elements = currentDoc.select(".products li")
      if (!elements.isEmpty()) {
         for (e in elements) {
            val productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "strong a.product-item-link", "href")
            val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".actions-primary form", "data-product-sku")
            val imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-wrapper img", Arrays.asList("src"), "https", "")
            val name = CrawlerUtils.scrapStringSimpleInfo(e, "strong .product-item-link ", false)
            val price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-wrapper span", null, false, ',', session, 0)
            var isAvailable: Boolean = price != 0

            var productRanking: RankingProduct? = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build()

            saveDataProduct(productRanking);
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   override fun hasNextPage(): Boolean {
      return currentDoc.select(".pages").isNotEmpty()
   }
}
