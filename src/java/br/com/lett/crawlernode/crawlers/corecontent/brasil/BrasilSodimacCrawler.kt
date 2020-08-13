package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import org.json.JSONArray
import org.jsoup.nodes.Document
import java.util.*

class BrasilSodimacCrawler(session: Session?) : Crawler(session) {
   override fun shouldVisit(): Boolean {
      val href = session.originalURL.toLowerCase()
      return !FILTERS.matcher(href).matches() && href.startsWith("https://www.sodimac.com.br/")
   }

   @ExperimentalStdlibApi
   override fun extractInformation(doc: Document): List<Product> {
      super.extractInformation(doc)
      val products: MutableList<Product> = ArrayList()
      if (doc.selectFirst(".product-basic-info") != null) {
         var productName = doc.selectFirst("h1.product-title").text()
         val productBrand = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-brand", true)
		  
         if(productName != null && productBrand != null) {
            productName = productBrand + " " + productName
			   }
		  
         products += product {
            url = session.originalURL
            internalId = doc.selectFirst(".product-cod").text().split(" ")[1]
            name = productName
			 
            val images = doc.select(".product-swatches img").eachAttr("src").map { img ->
               img.replace("wid=70&hei=70", "wid=420&hei=420")
            }.toMutableList()
            primaryImage = images.removeFirstOrNull()
            secondaryImages = JSONArray(images).toString()
            description = doc.htmlOf(".accordion")

            offer {
               sellerFullName = "Sodimac"
               isMainRetailer
               useSlugNameAsInternalSellerId
               val price = doc.selectFirst(".price").toDoubleComma()!!
               pricing {
                  creditCards = listOf(Card.VISA, Card.MAESTRO, Card.MAESTRO).toCreditCards(price)
                  spotlightPrice = price
                  bankSlip = spotlightPrice?.toBankSlip()
               }
            }
         }
      }
      return products
   }
}
