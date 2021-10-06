package br.com.lett.crawlernode.crawlers.ranking.keywords.salvador

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toInt

class SalvadorHiperidealCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 16
   }

   private val token: String by lazy {
      val doc = fetchDocument("https://www.hiperideal.com.br/${keywordEncoded.replace("+","%20")}")
      totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc,".resultado-busca-numero .value",true,0)
      CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"li[layout]","layout")
   }

   override fun extractProductsFromCurrentPage() {
      currentDoc = fetchDocument("https://www.hiperideal.com.br/buscapagina?ft=$keywordEncoded&PS=16&sl=$token&cc=4&sm=0&PageNumber=$currentPage")
      for (element in currentDoc.select(".product--large")) {
         val sku = element.selectFirst(".skuid")?.text()
         val url = CrawlerUtils.completeUrl(element.selectFirst(".product__title a")?.attr("href"), "https", "www.hiperideal.com.br") + "?sc=9";

         saveDataProduct(sku, null, url)
         log("Internalid $sku - Url $url")
      }
   }

}
