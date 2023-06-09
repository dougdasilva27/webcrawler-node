package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class SaopauloSupermercadotresmCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private val BASE_URL = "sm3m.ecommerce.bluesoft.com.br"

   override fun extractProductsFromCurrentPage() {
      pageSize = 32
      log("Página $currentPage")

      val url = "https://sm3m.ecommerce.bluesoft.com.br/products?keywords=${this.keywordEncoded}&page=${this.currentPage}"
      log("Link onde são feitos os crawlers: $url")

      currentDoc = fetchDocument(url)

      val products = currentDoc.select(".product")

      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts()
         }
         for (product in products) {

            val productUrl: String = CrawlerUtils.scrapUrl(product, ".images > a", "href", "https", BASE_URL)
            val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "#variant_id", "value")
            val name = CrawlerUtils.scrapStringSimpleInfo(product, "h5.product-title", true)
            val image = CrawlerUtils.scrapSimplePrimaryImage(product, "div.images img", listOf("src"), "https", "s3-sa-east-1.amazonaws.com")
            val isAvailable = product.selectFirst("button.btn.btn-secondary") != null
            val priceInCents = if (isAvailable) CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price-amount span", null, false, ',', session, null) else null

            val productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setImageUrl(image)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
               .build()

            saveDataProduct(productRanking)

            if (arrayProducts.size == productsLimit) {
               break
            }
         }
      } else {
         result = false
         log("Keyword sem resultado!")
      }
      log("Finalizando Crawler de produtos da página $currentPage até agora ${arrayProducts.size} produtos crawleados")
   }

   override fun hasNextPage(): Boolean {
      return currentDoc.selectFirst("#products-paginator a") != null
   }
}
