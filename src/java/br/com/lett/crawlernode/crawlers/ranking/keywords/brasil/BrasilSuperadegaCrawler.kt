package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class BrasilSuperadegaCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private val HOME_PAGE = "https://www.superadega.com.br/"

   override fun extractProductsFromCurrentPage() {
      pageSize = 21
      log("Página $currentPage")
      val url = "https://www.superadega.com.br/buscapagina?ft=$keywordEncoded&PS=21&sl=ce56140d-7c05-4428-a765-faaedbac10c8&cc=3&sm=0&PageNumber=$currentPage"
      log("Link onde são feitos os crawlers: $url")
      currentDoc = fetchDocument(url)
      val products = currentDoc.select(".n3colunas > ul > li:not([class=\"helperComplement\"])")
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts()
         }
         for (product in products) {
            val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".yv-review-quickreview", "value")
            val productUrl = CrawlerUtils.scrapUrl(product, ".collection-product-name > a", "href", "https://", HOME_PAGE)
            val name = CrawlerUtils.scrapStringSimpleInfo(product, ".collection-product-name", false)
            val image = CrawlerUtils.scrapSimplePrimaryImage(product, ".collection-product-image > a img", listOf("src"), "https", "superadega.vteximg.com.br")
            val price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".collection-product-price-price", null, false, ',', session, null)
            val available = price != null

            val productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(available)
               .build()

            saveDataProduct(productRanking)
            if (arrayProducts.size == productsLimit) break
         }
      } else {
         result = false
         log("Keyword sem resultado!")
      }
      log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados")
   }

   override fun setTotalProducts() {
      val url = "https://www.superadega.com.br/$keywordEncoded"
      val doc = fetchDocument(url)
      totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".resultado-busca-numero > .value", true, 0)
      log("Total da busca: $totalProducts")
   }
}
