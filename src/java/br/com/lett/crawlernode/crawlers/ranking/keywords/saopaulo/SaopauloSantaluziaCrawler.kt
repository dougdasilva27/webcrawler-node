package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import java.util.*

class SaopauloSantaluziaCrawler(session: Session) : CrawlerRankingKeywords(session) {

   override fun extractProductsFromCurrentPage() {
      pageSize = 48
      log("Página $currentPage")
      val url = "https://www.santaluzia.com.br/buscapagina?ft=$keywordEncoded&PS=48&sl=3c4f0b1f-8c42-410b-a078-96911a73069f&cc=48&sm=0&PageNumber=$currentPage"
      log("Link onde são feitos os crawlers: $url")
      currentDoc = fetchDocument(url)
      val products = currentDoc.select(".prateleira > ul > li > :not(helperComplement)")
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts()
         }
         for (product in products) {
            val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "article[data-id]", "data-id")
            val productUrl = CrawlerUtils.scrapUrl(product, "a", "href", "https://", "www.santaluzia.com.br")
            val name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", true);
            val imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".product-photo img", Arrays.asList("src"), "https", "")
            val price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".product-bestprice", null, false, ',', session, 0)

            val isAvailable = price != 0

            val productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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
      val url = "https://www.santaluzia.com.br/$keywordEncoded?&utmi_p=_---200-1039849_p&utmi_pc=BuscaFullText&utmi_cp=$keywordEncoded"
      val document = fetchDocument(url)
      totalProducts = CrawlerUtils.scrapIntegerFromHtml(document, ".resultado-busca-numero .value", true, 0)
      log("Total da busca: $totalProducts")
   }
}
